using System.Data;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Persistence;

namespace SmartFarm.Infrastructure.Identity;

// Single registration path shared by MVC and Android API: never grant farm permissions on signup.
public sealed class AccountRegistrationService(UserManager<SmartFarmUser> users,
    IFarmEnrollmentService enrollment, IUnitOfWork unit)
{
    public async Task<IdentityResult> RegisterAsync(string name, string email, string password,
        bool joinFarm = false, string? farmCode = null)
    {
        var cleanName = (name ?? "").Trim();
        var cleanEmail = (email ?? "").Trim();
        if (cleanName.Length is < 2 or > 120 || cleanEmail.Length is < 3 or > 256)
            return IdentityResult.Failed(new IdentityError { Description = "Invalid name or email length." });

        var user = new SmartFarmUser
        {
            FullName = cleanName,
            UserName = cleanEmail,
            Email = cleanEmail,
            LockoutEnabled = true
        };
        if (!joinFarm) return await users.CreateAsync(user, password);

        // Identity uniqueness checks must release read locks before parallel inserts; enrollment locks the farm explicitly.
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.ReadCommitted);
        try
        {
            var result = await users.CreateAsync(user, password);
            if (!result.Succeeded) return result;
            await enrollment.RequestAsync(user.Id, farmCode ?? "");
            await transaction.CommitAsync();
            return result;
        }
        catch (FarmOperationException error)
        {
            return IdentityResult.Failed(new IdentityError { Code = "FarmCode", Description = error.Message });
        }
        catch (DbUpdateException)
        {
            return IdentityResult.Failed(new IdentityError { Code = "Registration", Description = "Registration could not be saved. Please try again." });
        }
    }
}
