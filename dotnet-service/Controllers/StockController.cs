using DotnetService.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson;
using MongoDB.Driver;
using System.Security.Claims;

namespace DotnetService.Controllers;

[ApiController]
[Route("api/stocks")]
[Authorize]
public class StockController : ControllerBase
{
    private readonly IMongoDatabase _database;
    private readonly ILogger<StockController> _logger;

    public StockController(IMongoDatabase database, ILogger<StockController> logger)
    {
        _database = database;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<StockResponse>>> GetStocks(
        [FromQuery] int page = 0,
        [FromQuery] int size = 10)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_STOCKS_READ", "ROLE_STOCKS_CREATE",
                                       "ROLE_STOCKS_SAVE", "ROLE_STOCKS_DELETE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        var collection = _database.GetCollection<Stock>("stocks");
        var skip = page * size;

        _logger.LogDebug("Get all stocks - page: {Page}\t size: {Size}", page, size);

        var stocks = await collection.Find(_ => true)
            .Skip(skip)
            .Limit(size)
            .ToListAsync();

        var response = stocks.Select(StockResponse.FromStock).ToList();
        return Ok(response);
    }

    [HttpPost]
    public async Task<ActionResult<StockResponse>> CreateStock([FromBody] StockRequest request)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_STOCKS_CREATE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        var collection = _database.GetCollection<Stock>("stocks");
        var username = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "unknown";

        var stock = new Stock
        {
            Name = request.Name,
            Quantity = request.Quantity,
            Category = request.Category,
            CreatedDate = DateTime.UtcNow,
            LastModifiedDate = null,
            CreatedByUser = username,
            LastModifiedByUser = null,
            Price = request.Price,
            Currency = request.Currency
        };

        await collection.InsertOneAsync(stock);

        var inserted = await collection.Find(w => w.Id == stock.Id).FirstOrDefaultAsync();
        if (inserted == null)
        {
            return StatusCode(500, new ErrorResponse { Error = "Stock created but not found" });
        }

        return CreatedAtAction(nameof(GetStock), new { id = inserted.Id }, StockResponse.FromStock(inserted));
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<StockResponse>> GetStock(string id)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_STOCKS_READ", "ROLE_STOCKS_SAVE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Stock>("stocks");
        var stock = await collection.Find(w => w.Id == id).FirstOrDefaultAsync();

        if (stock == null)
        {
            return NotFound(new ErrorResponse { Error = "Stock not found" });
        }

        return Ok(StockResponse.FromStock(stock));
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<StockResponse>> UpdateStock(string id, [FromBody] StockRequest request)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_STOCKS_SAVE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Stock>("stocks");
        var username = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "unknown";

        var update = Builders<Stock>.Update
            .Set(w => w.Name, request.Name)
            .Set(w => w.Quantity, request.Quantity)
            .Set(w => w.Category, request.Category)
            .Set(w => w.Price, request.Price)
            .Set(w => w.Currency, request.Currency)
            .Set(w => w.LastModifiedDate, DateTime.UtcNow)
            .Set(w => w.LastModifiedByUser, username);

        var result = await collection.UpdateOneAsync(w => w.Id == id, update);

        if (result.MatchedCount == 0)
        {
            return NotFound(new ErrorResponse { Error = "Stock not found" });
        }

        var updated = await collection.Find(w => w.Id == id).FirstOrDefaultAsync();
        if (updated == null)
        {
            return StatusCode(500, new ErrorResponse { Error = "Stock updated but not found" });
        }

        return Ok(StockResponse.FromStock(updated));
    }

    [HttpDelete("{id}")]
    public async Task<ActionResult<MessageResponse>> DeleteStock(string id)
    {
        if (!CheckPermissions(new[] { "ROLE_ADMIN", "ROLE_STOCKS_DELETE", "SCOPE_openid" }))
        {
            return Forbid();
        }

        if (!ObjectId.TryParse(id, out _))
        {
            return BadRequest(new ErrorResponse { Error = "Invalid ID format" });
        }

        var collection = _database.GetCollection<Stock>("stocks");
        var result = await collection.DeleteOneAsync(w => w.Id == id);

        if (result.DeletedCount == 0)
        {
            return NotFound(new ErrorResponse { Error = "Stock not found" });
        }

        return Ok(new MessageResponse { Msg = $"Deleted stock id: {id}" });
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
