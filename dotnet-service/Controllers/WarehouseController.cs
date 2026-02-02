using DotnetService.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson;
using MongoDB.Driver;
using System.Security.Claims;

namespace DotnetService.Controllers;

[ApiController]
[Route("api/warehouses")]
[Authorize]
public class WarehouseController : ControllerBase
{
    private readonly IMongoDatabase _database;
    private readonly ILogger<WarehouseController> _logger;

    public WarehouseController(IMongoDatabase database, ILogger<WarehouseController> logger)
    {
        _database = database;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<PageResponse<WarehouseResponse>>> GetWarehouses(
        [FromQuery] int page = 0,
        [FromQuery] int size = 10)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_WAREHOUSES_READ", "ROLE_WAREHOUSES_CREATE",
                                       "ROLE_WAREHOUSES_SAVE", "ROLE_WAREHOUSES_DELETE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        var collection = _database.GetCollection<Warehouse>("warehouses");
        var skip = page * size;

        _logger.LogDebug("Get all warehouses - page: {Page}\t size: {Size}", page, size);

        var totalElements = await collection.CountDocumentsAsync(_ => true);
        var warehouses = await collection.Find(_ => true)
            .Skip(skip)
            .Limit(size)
            .ToListAsync();

        var content = warehouses.Select(WarehouseResponse.FromWarehouse).ToList();
        var totalPages = (int)Math.Ceiling((double)totalElements / size);

        var response = new PageResponse<WarehouseResponse>
        {
            Content = content,
            Number = page,
            Size = size,
            TotalPages = totalPages,
            TotalElements = totalElements,
            First = page == 0,
            Last = page >= totalPages - 1
        };

        return Ok(response);
    }

    [HttpPost]
    public async Task<ActionResult<WarehouseResponse>> CreateWarehouse([FromBody] WarehouseRequest request)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_WAREHOUSES_CREATE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        var collection = _database.GetCollection<Warehouse>("warehouses");
        var username = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "unknown";

        var warehouse = new Warehouse
        {
            Name = request.Name,
            Location = request.Location,
            Capacity = request.Capacity,
            CreatedDate = DateTime.UtcNow,
            LastModifiedDate = null,
            CreatedByUser = username,
            LastModifiedByUser = null
        };

        await collection.InsertOneAsync(warehouse);

        var inserted = await collection.Find(w => w.Id == warehouse.Id).FirstOrDefaultAsync();
        if (inserted == null)
        {
            return StatusCode(500, new ErrorResponse { Error = "Warehouse created but not found" });
        }

        return CreatedAtAction(nameof(GetWarehouse), new { id = inserted.Id }, WarehouseResponse.FromWarehouse(inserted));
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<WarehouseResponse>> GetWarehouse(string id)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_WAREHOUSES_READ", "ROLE_WAREHOUSES_SAVE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Warehouse>("warehouses");
        var warehouse = await collection.Find(w => w.Id == id).FirstOrDefaultAsync();

        if (warehouse == null)
        {
            return NotFound(new ErrorResponse { Error = "Warehouse not found" });
        }

        return Ok(WarehouseResponse.FromWarehouse(warehouse));
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<WarehouseResponse>> UpdateWarehouse(string id, [FromBody] WarehouseRequest request)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_WAREHOUSES_SAVE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Warehouse>("warehouses");
        var username = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "unknown";

        var update = Builders<Warehouse>.Update
            .Set(w => w.Name, request.Name)
            .Set(w => w.Location, request.Location)
            .Set(w => w.Capacity, request.Capacity)
            .Set(w => w.LastModifiedDate, DateTime.UtcNow)
            .Set(w => w.LastModifiedByUser, username);

        var result = await collection.UpdateOneAsync(w => w.Id == id, update);

        if (result.MatchedCount == 0)
        {
            return NotFound(new ErrorResponse { Error = "Warehouse not found" });
        }

        var updated = await collection.Find(w => w.Id == id).FirstOrDefaultAsync();
        if (updated == null)
        {
            return StatusCode(500, new ErrorResponse { Error = "Warehouse updated but not found" });
        }

        return Ok(WarehouseResponse.FromWarehouse(updated));
    }

    [HttpDelete("{id}")]
    public async Task<ActionResult<MessageResponse>> DeleteWarehouse(string id)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_WAREHOUSES_DELETE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Warehouse>("warehouses");
        var result = await collection.DeleteOneAsync(w => w.Id == id);

        if (result.DeletedCount == 0)
        {
            return NotFound(new ErrorResponse { Error = "Warehouse not found" });
        }

        return Ok(new MessageResponse { Msg = $"Deleted warehouse id: {id}" });
    }

    private bool CheckPermissions(string[] requiredRoles)
    {
        // Try to get authorities as multiple claims (JWT array)
        var authoritiesClaims = User.Claims.Where(c => c.Type == "authorities").Select(c => c.Value).ToList();

        if (authoritiesClaims.Any())
        {
            _logger.LogDebug("Found {Count} authority claims: {Authorities}", authoritiesClaims.Count, string.Join(", ", authoritiesClaims));
            return requiredRoles.Any(role => authoritiesClaims.Contains(role));
        }

        // Fallback: try as comma-separated string
        var authoritiesString = User.FindFirst("authorities")?.Value;
        if (!string.IsNullOrEmpty(authoritiesString))
        {
            var authorities = authoritiesString.Split(',').Select(a => a.Trim()).ToList();
            _logger.LogDebug("Found authority string: {Authorities}", authoritiesString);
            return requiredRoles.Any(role => authorities.Contains(role));
        }

        _logger.LogWarning("No authorities claim found in JWT token");
        return false;
    }
}
