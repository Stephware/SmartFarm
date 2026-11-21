using System.Data;
using System.Reflection;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Infrastructure.Services.Monitoring;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class FieldworkStockServiceTests
{
    private static readonly Guid Farm = Guid.NewGuid();
    private static readonly Guid Cycle = Guid.NewGuid();
    private static readonly Guid Actor = Guid.NewGuid();
    private static readonly Guid Item = Guid.NewGuid();
    private static readonly Guid Activity = Guid.NewGuid();

    private static InventoryItemDetails Stock(InventoryCategory category) =>
        new(Item, Farm, "Supply", category, "kg", null, 5m, null, false,
            null, "rowversion");

    private static RecordActivityWithStockInput Input(byte type = 2, decimal quantity = 1.5m) =>
        new()
        {
            Activity = new RecordActivityInput { ActivityType = type, Notes = "Field application" },
            InventoryItemId = Item, QuantityUsed = quantity, RowVersion = "rowversion"
        };

    [Fact]
    public async Task CombinedRecordLinksNewActivityAndNegativeStockMovement()
    {
        var transaction = new TestTransaction();
        var unit = new TestUnit(transaction);
        InventoryMovementInput? posted = null;
        var monitoring = Stub<IMonitoringService>((method, _) =>
            method == nameof(IMonitoringService.RecordActivityAsync)
                ? Task.FromResult(new ActivityDetails(Activity, 2, DateTime.UtcNow,
                    DateTime.UtcNow, Actor, null))
                : throw new NotSupportedException(method));
        var inventory = Stub<IInventoryService>((method, args) => method switch
        {
            nameof(IInventoryService.GetAsync) => Task.FromResult<InventoryItemDetails?>(
                Stock(InventoryCategory.Fertilizers)),
            nameof(IInventoryService.PostAsync) => Post(args),
            _ => throw new NotSupportedException(method)
        });
        Task<InventoryItemDetails> Post(object?[] args)
        {
            posted = Assert.IsType<InventoryMovementInput>(args[3]);
            return Task.FromResult(Stock(InventoryCategory.Fertilizers));
        }

        var saved = await new FieldworkStockService(monitoring, inventory, unit)
            .RecordAsync(Farm, Cycle, Actor, Input());

        Assert.Equal(Activity, saved.Activity.Id);
        Assert.Equal(Item, saved.Inventory.Id);
        Assert.NotNull(posted);
        Assert.Equal(StockTransactionType.Consumption, posted.TransactionType);
        Assert.Equal(-1.5m, posted.QuantityChange);
        Assert.Equal(Activity, posted.CropActivityId);
        Assert.Equal("rowversion", posted.RowVersion);
        Assert.Equal(IsolationLevel.Serializable, unit.Level);
        Assert.True(transaction.Committed);
        Assert.True(transaction.Disposed);
    }

    [Fact]
    public async Task WrongCategoryRejectsBeforeActivityIsCreated()
    {
        var transaction = new TestTransaction();
        var unit = new TestUnit(transaction);
        bool wroteActivity = false;
        var monitoring = Stub<IMonitoringService>((method, _) =>
        {
            wroteActivity = true;
            throw new NotSupportedException(method);
        });
        var inventory = Stub<IInventoryService>((method, _) =>
            method == nameof(IInventoryService.GetAsync)
                ? Task.FromResult<InventoryItemDetails?>(
                    Stock(InventoryCategory.Pesticides))
                : throw new NotSupportedException(method));

        await Assert.ThrowsAsync<FarmOperationException>(() =>
            new FieldworkStockService(monitoring, inventory, unit)
                .RecordAsync(Farm, Cycle, Actor, Input()));
        Assert.False(wroteActivity);
        Assert.False(transaction.Committed);
        Assert.True(transaction.Disposed);
    }

    [Fact]
    public async Task StockConflictDoesNotCommitActivityOrTaskChanges()
    {
        var transaction = new TestTransaction();
        var unit = new TestUnit(transaction);
        bool wroteActivity = false;
        var monitoring = Stub<IMonitoringService>((method, _) =>
        {
            wroteActivity = true;
            return Task.FromResult(new ActivityDetails(Activity, 2, DateTime.UtcNow,
                DateTime.UtcNow, Actor, null));
        });
        var inventory = Stub<IInventoryService>((method, _) => method switch
        {
            nameof(IInventoryService.GetAsync) => Task.FromResult<InventoryItemDetails?>(
                Stock(InventoryCategory.Fertilizers)),
            nameof(IInventoryService.PostAsync) => throw new FarmOperationException(
                FarmError.Conflict, "Inventory changed."),
            _ => throw new NotSupportedException(method)
        });

        await Assert.ThrowsAsync<FarmOperationException>(() =>
            new FieldworkStockService(monitoring, inventory, unit)
                .RecordAsync(Farm, Cycle, Actor, Input()));
        Assert.True(wroteActivity);
        Assert.False(transaction.Committed);
        Assert.True(transaction.Disposed);
    }

    [Theory]
    [InlineData((byte)1, 1.0, false)]
    [InlineData((byte)2, 0.0, false)]
    [InlineData((byte)3, -1.0, false)]
    [InlineData((byte)2, 0.0001, false)]
    [InlineData((byte)2, 1.250, true)]
    public async Task RejectInvalidActivityTypesOrStockQuantities(
        byte activityType, double quantity, bool valid)
    {
        var unit = new TestUnit(new TestTransaction());
        var monitoring = Stub<IMonitoringService>((method, _) =>
            Task.FromResult(new ActivityDetails(Activity, activityType, DateTime.UtcNow,
                DateTime.UtcNow, Actor, null)));
        var inventory = Stub<IInventoryService>((method, _) => method switch
        {
            nameof(IInventoryService.GetAsync) => Task.FromResult<InventoryItemDetails?>(
                Stock(activityType == 3 ? InventoryCategory.Pesticides :
                    InventoryCategory.Fertilizers)),
            nameof(IInventoryService.PostAsync) => Task.FromResult(
                Stock(activityType == 3 ? InventoryCategory.Pesticides :
                    InventoryCategory.Fertilizers)),
            _ => throw new NotSupportedException(method)
        });
        var service = new FieldworkStockService(monitoring, inventory, unit);
        var input = Input(activityType, (decimal)quantity);
        if (!valid)
            await Assert.ThrowsAsync<FarmOperationException>(() =>
                service.RecordAsync(Farm, Cycle, Actor, input));
        else
            Assert.Equal(Activity, (await service.RecordAsync(Farm, Cycle, Actor, input)).Activity.Id);
    }

    private static T Stub<T>(Func<string, object?[], object> invoke) where T : class
    {
        var value = DispatchProxy.Create<T, ServiceProxy>();
        ((ServiceProxy)(object)value).Call = invoke;
        return value;
    }

    public class ServiceProxy : DispatchProxy
    {
        public Func<string, object?[], object>? Call { get; set; }
        protected override object? Invoke(MethodInfo? method, object?[]? arguments) =>
            Call!(method!.Name, arguments ?? []);
    }

    private sealed class TestUnit(TestTransaction transaction) : IUnitOfWork
    {
        public bool HasActiveTransaction => false;
        public IsolationLevel? Level { get; private set; }
        public Task<int> SaveChangesAsync(CancellationToken ct = default) => Task.FromResult(0);
        public Task<IUnitOfWorkTransaction> BeginTransactionAsync(
            IsolationLevel level, CancellationToken ct = default)
        {
            Level = level;
            return Task.FromResult<IUnitOfWorkTransaction>(transaction);
        }
    }

    private sealed class TestTransaction : IUnitOfWorkTransaction
    {
        public bool Committed { get; private set; }
        public bool Disposed { get; private set; }
        public Task CommitAsync(CancellationToken ct = default)
        {
            Committed = true;
            return Task.CompletedTask;
        }
        public ValueTask DisposeAsync()
        {
            Disposed = true;
            return ValueTask.CompletedTask;
        }
    }
}
