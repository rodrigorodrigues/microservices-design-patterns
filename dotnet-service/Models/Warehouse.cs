using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace DotnetService.Models;

public class Warehouse
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("name")]
    public required string Name { get; set; }

    [BsonElement("location")]
    public required string Location { get; set; }

    [BsonElement("capacity")]
    public int Capacity { get; set; }

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
}

public class WarehouseRequest
{
    public required string Name { get; set; }
    public required string Location { get; set; }
    public int Capacity { get; set; }
}

public class WarehouseResponse
{
    public string? Id { get; set; }
    public required string Name { get; set; }
    public required string Location { get; set; }
    public int Capacity { get; set; }
    public string? CreatedDate { get; set; }
    public string? LastModifiedDate { get; set; }
    public string? CreatedByUser { get; set; }
    public string? LastModifiedByUser { get; set; }

    public static WarehouseResponse FromWarehouse(Warehouse warehouse)
    {
        return new WarehouseResponse
        {
            Id = warehouse.Id,
            Name = warehouse.Name,
            Location = warehouse.Location,
            Capacity = warehouse.Capacity,
            CreatedDate = warehouse.CreatedDate?.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
            LastModifiedDate = warehouse.LastModifiedDate?.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
            CreatedByUser = warehouse.CreatedByUser,
            LastModifiedByUser = warehouse.LastModifiedByUser
        };
    }
}
