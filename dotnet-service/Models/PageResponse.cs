namespace DotnetService.Models;

public class PageResponse<T>
{
    public List<T> Content { get; set; } = new();
    public int Number { get; set; }
    public int Size { get; set; }
    public int TotalPages { get; set; }
    public long TotalElements { get; set; }
    public bool First { get; set; }
    public bool Last { get; set; }
}
