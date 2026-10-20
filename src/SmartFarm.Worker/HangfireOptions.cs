namespace SmartFarm.Worker;

public sealed class HangfireOptions
{
    public bool Enabled { get; set; }
    public int WorkerCount { get; set; } = 1;
    public bool InitializeSchema { get; set; } = false;
}
