using System.Text.Json;
using System.Text.Json.Serialization;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts;

// Allow clear symbolic JSON input while preserving the existing numeric JSON
// representation and SQL codes used by deployed/offline clients.
public abstract class NamedByteCodeConverter<TCode> : JsonConverter<byte>
    where TCode : struct, Enum
{
    public override byte Read(ref Utf8JsonReader reader, Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number && reader.TryGetByte(out var numeric))
            return numeric;

        if (reader.TokenType == JsonTokenType.String)
        {
            var name = reader.GetString()?.Trim();
            if (!string.IsNullOrWhiteSpace(name))
            {
                // Web JSON options already accepted numeric strings; preserve that behavior.
                if (byte.TryParse(name, System.Globalization.NumberStyles.None,
                        System.Globalization.CultureInfo.InvariantCulture, out var numericString))
                    return numericString;
                foreach (var value in Enum.GetValues<TCode>())
                {
                    if (string.Equals(Normalize(value.ToString()), Normalize(name),
                            StringComparison.OrdinalIgnoreCase))
                        return Convert.ToByte(value);
                }
            }
        }

        throw new JsonException($"Expected a valid {typeof(TCode).Name} name or numeric code.");
    }

    public override void Write(Utf8JsonWriter writer, byte value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(value);

    private static string Normalize(string value) =>
        new(value.Where(char.IsLetterOrDigit).ToArray());
}

public sealed class ActivityTypeJsonConverter : NamedByteCodeConverter<FarmActivityType> { }
public sealed class HealthStatusJsonConverter : NamedByteCodeConverter<CropHealthStatus> { }
public sealed class TaskTypeJsonConverter : NamedByteCodeConverter<FarmTaskType> { }
public sealed class TaskCloseStatusJsonConverter : NamedByteCodeConverter<FarmTaskStatus> { }
public sealed class MemberRoleJsonConverter : NamedByteCodeConverter<FarmMemberRole> { }

public sealed class NamedEnumCodeConverter<TCode> : JsonConverter<TCode>
    where TCode : struct, Enum
{
    public override TCode Read(ref Utf8JsonReader reader, Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number && reader.TryGetByte(out var numeric))
            return (TCode)Enum.ToObject(typeof(TCode), numeric);

        if (reader.TokenType == JsonTokenType.String)
        {
            var name = reader.GetString()?.Trim();
            if (!string.IsNullOrWhiteSpace(name) &&
                Enum.TryParse<TCode>(name, ignoreCase: true, out var parsed) &&
                Enum.IsDefined(parsed))
                return parsed;
        }

        throw new JsonException($"Expected a valid {typeof(TCode).Name} name or numeric code.");
    }

    public override void Write(Utf8JsonWriter writer, TCode value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(Convert.ToByte(value));
}
