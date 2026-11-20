FROM mcr.microsoft.com/dotnet/aspnet:10.0-noble@sha256:12b1135f5f8b3c0a214f2a680ff2033dff4f684f445d6c697d9524ceeb303586
ARG SERVICE
WORKDIR /app
COPY ca.crt /usr/local/share/ca-certificates/anigo-sql.crt
RUN update-ca-certificates
COPY ${SERVICE}/ ./
ENV APP_DLL=SmartFarm.${SERVICE}.dll
USER 1654:1654
ENTRYPOINT ["sh", "-c", "exec dotnet \"$APP_DLL\""]
