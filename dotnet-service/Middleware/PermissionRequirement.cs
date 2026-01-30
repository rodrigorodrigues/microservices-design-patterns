using Microsoft.AspNetCore.Authorization;

namespace DotnetService.Middleware;

public class PermissionRequirement : IAuthorizationRequirement
{
    public string[] RequiredRoles { get; }

    public PermissionRequirement(params string[] requiredRoles)
    {
        RequiredRoles = requiredRoles;
    }
}

public class PermissionHandler : AuthorizationHandler<PermissionRequirement>
{
    protected override Task HandleRequirementAsync(
        AuthorizationHandlerContext context,
        PermissionRequirement requirement)
    {
        var authoritiesClaim = context.User.FindFirst("authorities");
        if (authoritiesClaim == null)
        {
            return Task.CompletedTask;
        }

        var authorities = authoritiesClaim.Value.Split(',')
            .Select(a => a.Trim())
            .ToList();

        if (requirement.RequiredRoles.Any(role => authorities.Contains(role)))
        {
            context.Succeed(requirement);
        }

        return Task.CompletedTask;
    }
}
