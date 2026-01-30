# Build stage
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS builder

ARG PORT

WORKDIR /app

# Copy csproj and restore dependencies
COPY DotnetService.csproj ./
RUN dotnet restore DotnetService.csproj

# Copy source code
COPY . ./

# Build application
RUN dotnet publish DotnetService.csproj -c Release -o out

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:10.0

ARG PORT
ENV SERVER_PORT=$PORT
ENV ASPNETCORE_URLS=http://+:${PORT}

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/out .

EXPOSE $PORT

ENTRYPOINT ["dotnet", "DotnetService.dll"]
