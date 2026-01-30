using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace DotnetService.Models;

public class Stock
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("name")]
    public required string Name { get; set; }

    [BsonElement("quantity")]
    public int Quantity { get; set; }

    [BsonElement("category")]
    public required string Category { get; set; }

    [BsonElement("createdDate")]
    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime? CreatedDate { get; set; }

    [BsonElement("lastModifiedDate")]
    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime? LastModifiedDate { get; set; }

    [BsonElement("createdByUser")]
    public string? CreatedByUser { get; set; }

    [BsonElement("lastModifiedByUser")]
    public string? LastModifiedByUser { get; set; }

    [BsonElement("price")]
    public double Price { get; set; }

    [BsonElement("currency")]
    public required string Currency { get; set; }
}

public class StockRequest
{
    public required string Name { get; set; }
    public int Quantity { get; set; }
    public required string Category { get; set; }
    public double Price { get; set; }
    public required string Currency { get; set; }
}

public class StockResponse
{
    public string? Id { get; set; }
    public required string Name { get; set; }
    public int Quantity { get; set; }
    public required string Category { get; set; }
    public string? CreatedDate { get; set; }
    public string? LastModifiedDate { get; set; }
    public string? CreatedByUser { get; set; }
    public string? LastModifiedByUser { get; set; }
    public double Price { get; set; }
    public required string Currency { get; set; }

    public static StockResponse FromStock(Stock stock)
    {
        return new StockResponse
        {
            Id = stock.Id,
            Name = stock.Name,
            Quantity = stock.Quantity,
            Category = stock.Category,
            CreatedDate = stock.CreatedDate?.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
            LastModifiedDate = stock.LastModifiedDate?.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
            CreatedByUser = stock.CreatedByUser,
            LastModifiedByUser = stock.LastModifiedByUser,
            Price = stock.Price,
            Currency = stock.Currency
        };
    }
}

public class ErrorResponse
{
    public required string Error { get; set; }
}

public class MessageResponse
{
    public required string Msg { get; set; }
}
