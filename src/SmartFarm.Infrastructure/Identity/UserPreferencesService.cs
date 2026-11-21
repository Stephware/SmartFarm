using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Services;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Identity;

// Existing Identity claims persist preferences without adding a user column or changing token contracts.
public sealed class UserPreferencesService(SmartFarmDbContext db)
{
    public const string LanguageClaim = "anigo:language";
    public async Task<string> LanguageAsync(Guid user, CancellationToken ct = default) =>
        BackendMessages.Language(await db.UserClaims.AsNoTracking().Where(c => c.UserId == user && c.ClaimType == LanguageClaim)
            .OrderByDescending(c => c.Id).Select(c => c.ClaimValue).FirstOrDefaultAsync(ct));

    public async Task SetLanguageAsync(Guid user, string language, CancellationToken ct = default)
    {
        if (language is not ("en" or "fil")) throw new FarmOperationException(FarmError.InvalidInput, "Choose en or fil.");
        if (!await db.Users.AnyAsync(u => u.Id == user && u.DisabledAtUtc == null, ct))
            throw new FarmOperationException(FarmError.NotFound, "Account not found.");
        await using var transaction = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var old = await db.UserClaims.Where(c => c.UserId == user && c.ClaimType == LanguageClaim).ToListAsync(ct);
        db.UserClaims.RemoveRange(old);
        db.UserClaims.Add(new IdentityUserClaim<Guid> { UserId = user, ClaimType = LanguageClaim, ClaimValue = language });
        await db.SaveChangesAsync(ct);
        await transaction.CommitAsync(ct);
    }
}
