using SmartFarm.Infrastructure.Identity;
using Xunit;
namespace SmartFarm.Api.Tests;
public sealed class AccountRecoveryTests
{
    [Theory]
    [InlineData("https://farmer.example.test", false, true)]
    [InlineData("http://localhost:5080", true, true)]
    [InlineData("http://farmer.example.test", false, false)]
    [InlineData("https://user:pass@farmer.example.test", false, false)]
    [InlineData("https://farmer.example.test/path", false, false)]
    [InlineData("https://farmer.example.test?evil=1", false, false)]
    [InlineData("https://farmer.example.test/#evil", false, false)]
    public void RecoveryOriginRequiresConfiguredTrustedOrigin(string origin, bool development, bool valid) =>
        Assert.Equal(valid, new AccountRecoveryOptions { ApplicationOrigin = origin }.ValidOrigin(development));
}
