using System.Net.Http.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Development;
using SmartFarm.Infrastructure.Persistence;
using Xunit;
namespace SmartFarm.Api.Tests;
public sealed class LargePageSqlTests(BackendSqlFixture fixture) : IClassFixture<BackendSqlFixture>
{
    [Fact]
    public async Task LargeTiedTaskListsKeepStablePagesAndExactFilteredCountsAcrossDstDate()
    {
        if (fixture.Factory is null) return;
        using var scope = fixture.Factory.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
        var farm = new Farm { OwnerUserId = DemoDataset.Id("owner"), Name = "Paging fixture " + Guid.NewGuid().ToString("N"),
            LocationText = "Synthetic SQL test", TimeZoneId = "America/Sao_Paulo", CreatedAtUtc = DateTime.UtcNow };
        db.Farms.Add(farm);
        for (var n=0; n<230; n++) db.FarmTasks.Add(new FarmTask { FarmId=farm.Id, TaskType=6, DueDate=new(2018,11,4),
            Description="Page fixture", CreatedAtUtc=DateTime.UtcNow });
        await db.SaveChangesAsync();
        using var client = await fixture.Client("owner");
        var root = $"/api/farms/{farm.Id}/records/tasks?pageSize=100&sort=asc&from=2018-11-04&to=2018-11-04&search=fixture";
        var all = new List<Guid>();
        for (var page=1; page<=3; page++)
        {
            var result = (await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + $"&page={page}"))!;
            Assert.Equal(230,result.TotalCount); Assert.Equal(page<3,result.HasMore);
            Assert.Equal(page<3?100:30,result.Items.Count); all.AddRange(result.Items.Select(t=>t.Id));
        }
        Assert.Equal(230,all.Distinct().Count());
        var repeat = (await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + "&page=2"))!;
        Assert.Equal(all.Skip(100).Take(100),repeat.Items.Select(t=>t.Id));
    }
}
