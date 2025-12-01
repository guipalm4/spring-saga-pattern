#!/bin/sh
set -e

echo "🚀 Iniciando aplicação Saga E-commerce..."
echo "📅 Data/Hora: $(date)"
echo "🏷️  Perfil ativo: ${SPRING_PROFILES_ACTIVE:-default}"
echo "🔧 Java Options: ${JAVA_OPTS}"

if [ -n "$WAIT_FOR_SERVICES" ]; then
    echo "⏳ Aguardando serviços: $WAIT_FOR_SERVICES"

    for service in $(echo $WAIT_FOR_SERVICES | tr "," "\n"); do
        host=$(echo $service | cut -d: -f1)
        port=$(echo $service | cut -d: -f2)

        echo "🔍 Verificando $host:$port..."

        while ! nc -z $host $port; do
            echo "⏳ Aguardando $host:$port estar disponível..."
            sleep 2
        done

        echo "✅ $host:$port está disponível!"
    done
fi

if [ "$SPRING_PROFILES_ACTIVE" = "local" ] || [ "$SPRING_PROFILES_ACTIVE" = "docker" ]; then
    LOCALSTACK_HOST=${AWS_LOCALSTACK_ENDPOINT:-https://localstack:4566}
    echo "🔍 Verificando LocalStack em $LOCALSTACK_HOST..."

    until curl -k -s $LOCALSTACK_HOST/health | grep -q "running"; do
        echo "⏳ Aguardando LocalStack estar pronto..."
        sleep 3
    done

    echo "✅ LocalStack está pronto!"
fi

if [ -n "$TZ" ]; then
    echo "🌍 Configurando timezone para: $TZ"
    export TZ
fi

JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"
JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}"

echo "🔧 Configurações finais:"
echo "   - Java Options: $JAVA_OPTS"
echo "   - Perfil Spring: $SPRING_PROFILES_ACTIVE"
echo "   - Porta: $SERVER_PORT"
echo "   - Timezone: $TZ"

echo "🎯 Executando comando: $@"

exec "$@"