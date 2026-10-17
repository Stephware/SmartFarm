namespace SmartFarm.Core.Contracts.Farms;

public enum FarmError
{
    NotFound,
    Conflict,
    InvalidInput
}

public sealed class FarmOperationException(FarmError error, string message) : Exception(message)
{
    public FarmError Error { get; } = error;
}

