using Microsoft.IdentityModel.Tokens;

namespace DotnetService.Services;

public interface IJwksService
{
    Task<SecurityKey?> GetSigningKeyAsync(string jwksUrl, string? kid);
}
