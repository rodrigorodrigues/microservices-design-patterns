using DotnetService.Configuration;
using DotnetService.Data;
using DotnetService.Middleware;
using DotnetService.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using MongoDB.Driver;
using Prometheus;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Load configuration
var appConfig = new AppConfig(builder.Configuration);

// Load Consul configuration if enabled
if (appConfig.ConsulEnabled)
{
    try
    {
        var loggerFactory = LoggerFactory.Create(config => config.AddConsole());
        var tempConsulService = new ConsulService(loggerFactory.CreateLogger<ConsulService>());
        var tempLogger = loggerFactory.CreateLogger<AppConfig>();

        var consulYamlConfig = await tempConsulService.ReadYamlConfigAsync($"{appConfig.ConsulKvPath}/{appConfig.Profile}");
        if (consulYamlConfig != null)
        {
            tempLogger.LogInformation("Applying YAML configuration from Consul");

            // Create a new AppConfig with logger to apply Consul config
            var configWithLogger = new AppConfig(builder.Configuration, tempLogger)
            {
                ServerPort = appConfig.ServerPort,
                MongoDbUri = appConfig.MongoDbUri,
                AppName = appConfig.AppName,
                SecretToken = appConfig.SecretToken,
                ConsulEnabled = appConfig.ConsulEnabled,
                ConsulServer = appConfig.ConsulServer,
                ConsulPort = appConfig.ConsulPort,
                HostName = appConfig.HostName,
                ConsulKvPath = appConfig.ConsulKvPath,
                Profile = appConfig.Profile,
                JwksValidation = appConfig.JwksValidation,
                JwksUrl = appConfig.JwksUrl,
                Algorithm = appConfig.Algorithm
            };

            configWithLogger.ApplyConsulYamlConfig(consulYamlConfig);

            // Copy values back
            appConfig.SecretToken = configWithLogger.SecretToken;
            appConfig.JwksValidation = configWithLogger.JwksValidation;
            appConfig.JwksUrl = configWithLogger.JwksUrl;
            appConfig.Algorithm = configWithLogger.Algorithm;
        }
    }
    catch (Exception ex)
    {
        Console.WriteLine($"Failed to load Consul config: {ex.Message}");
    }
}

// Configure MongoDB
builder.Services.AddSingleton<IMongoClient>(sp =>
{
    return new MongoClient(appConfig.MongoDbUri);
});

builder.Services.AddScoped(sp =>
{
    var client = sp.GetRequiredService<IMongoClient>();
    return client.GetDatabase("stockdb");
});

// Configure JWT Authentication
if (appConfig.JwksValidation)
{
    builder.Services.AddSingleton<IJwksService, JwksService>();
    builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
        .AddJwtBearer(options =>
        {
            options.TokenValidationParameters = new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                ValidateIssuer = false,
                ValidateAudience = false,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            };

            options.Events = new JwtBearerEvents
            {
                OnMessageReceived = context =>
                {
                    var jwksService = context.HttpContext.RequestServices.GetRequiredService<IJwksService>();
                    var token = context.Request.Headers["Authorization"].FirstOrDefault()?.Split(" ").Last();

                    if (!string.IsNullOrEmpty(token))
                    {
                        var handler = new System.IdentityModel.Tokens.Jwt.JwtSecurityTokenHandler();
                        var jwtToken = handler.ReadJwtToken(token);
                        var kid = jwtToken.Header.Kid;

                        var signingKey = jwksService.GetSigningKeyAsync(appConfig.JwksUrl, kid).Result;
                        if (signingKey != null)
                        {
                            context.Options.TokenValidationParameters.IssuerSigningKey = signingKey;
                        }
                    }

                    return Task.CompletedTask;
                }
            };
        });
}
else
{
    builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
        .AddJwtBearer(options =>
        {
            options.TokenValidationParameters = new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(appConfig.SecretToken)),
                ValidateIssuer = false,
                ValidateAudience = false,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            };
        });
}

builder.Services.AddAuthorization();

// Register Consul service
if (appConfig.ConsulEnabled)
{
    builder.Services.AddSingleton<IConsulService, ConsulService>();
}

builder.Services.AddSingleton(appConfig);
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "DotNet Microservice API",
        Version = "v1",
        Description = "A microservice for managing stocks built with ASP.NET Core and MongoDB"
    });

    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "JWT Authorization header using the Bearer scheme",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(builder =>
    {
        builder.AllowAnyOrigin()
               .AllowAnyMethod()
               .AllowAnyHeader();
    });
});

var app = builder.Build();

// Register service with Consul
if (appConfig.ConsulEnabled)
{
    var consulService = app.Services.GetRequiredService<IConsulService>();
    await consulService.RegisterServiceAsync(appConfig);
    app.Logger.LogInformation("Service registered with Consul");
}

app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "DotNet Microservice API v1");
    c.RoutePrefix = "swagger-ui";
});

app.UseCors();
app.UseRouting();
app.UseHttpMetrics();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.MapMetrics("/actuator/prometheus");

app.Logger.LogInformation("Starting {AppName} on port {Port}", appConfig.AppName, appConfig.ServerPort);

app.Run($"http://0.0.0.0:{appConfig.ServerPort}");
