using Microsoft.AspNetCore.Mvc;
using DotnetService.Configuration;

namespace DotnetService.Controllers;

[ApiController]
[Route("actuator")]
public class ActuatorController : ControllerBase
{
    private readonly AppConfig _config;

    public ActuatorController(AppConfig config)
    {
        _config = config;
    }

    [HttpGet]
    public IActionResult GetActuator()
    {
        var port = _config.ServerPort;
        var actuator = new
        {
            _links = new
            {
                self = new
                {
                    href = $"http://localhost:{port}/actuator",
                    templated = false
                },
                health = new
                {
                    href = $"http://localhost:{port}/actuator/health",
                    templated = false
                },
                info = new
                {
                    href = $"http://localhost:{port}/actuator/info",
                    templated = false
                },
                prometheus = new
                {
                    href = $"http://localhost:{port}/actuator/prometheus",
                    templated = false
                }
            }
        };

        return Ok(actuator);
    }

    [HttpGet("health")]
    public IActionResult GetHealth()
    {
        return Ok(new { status = "UP" });
    }

    [HttpGet("info")]
    public IActionResult GetInfo()
    {
        return Ok(new { });
    }
}
