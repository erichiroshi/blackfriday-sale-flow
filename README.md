# Black Friday Sale Flow

Sistema de venda flash (Black Friday) com **Fluxo Síncrono** (reserva de estoque
em milissegundos via Redis) e **Fluxo Assíncrono** (persistência em lote no
Postgres via RabbitMQ). Arquitetura Hexagonal (Ports & Adapters).

## Rodando localmente

```bash
docker compose up -d          # sobe Postgres, Redis, RabbitMQ
./gradlew bootRun             # sobe a aplicação (porta 8080)
```

A aplicação semeia automaticamente 100 unidades para `PRODUCT-1` no Redis
no startup (`StockSeedRunner`), sem sobrescrever se a chave já existir.

## Fluxo síncrono — reservar estoque

```bash
curl -i -X POST http://localhost:8080/products/PRODUCT-1/reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"customerId": "customer-42"}'
```

Resposta esperada (202 Accepted):

```json
{
  "orderId": "5b1f...",
  "status": "RESERVED",
  "idempotentReplay": false,
  "statusUrl": "/orders/5b1f..."
}
```

Quando o estoque acabou (409 Conflict):

```json
{
  "error": "OUT_OF_STOCK",
  "message": "Product PRODUCT-1 is out of stock",
  "timestamp": "2026-11-27T00:00:01Z"
}
```

Reenviar a mesma requisição com o **mesmo** header `Idempotency-Key` nunca
decrementa o estoque de novo — retorna o mesmo `orderId` com
`idempotentReplay: true`.

## Consultar status do pedido

```bash
curl http://localhost:8080/orders/{orderId}
```

```json
{ "orderId": "5b1f...", "status": "CONFIRMED" }
```

`status` pode ser `RESERVED` (ainda não persistido pelo worker),
`CONFIRMED` (persistido com sucesso) ou `FAILED` (persistência falhou após
3 tentativas; estoque já foi devolvido ao Redis).

## Arquitetura

```
domain/            <- Order, ProductId, OrderId, CustomerId, IdempotencyKey,
                       OrderStatus, StockReservationResult (Java puro, zero Spring)
application/
  port/in/         <- ReserveStockUseCase, GetOrderStatusUseCase, PersistOrderBatchUseCase
  port/out/        <- StockCachePort, IdempotencyPort, OrderQueuePort, OrderPersistencePort
  usecase/         <- implementações (também sem anotações Spring)
infrastructure/
  adapter/in/web/       <- ReservationController, OrderStatusController, GlobalExceptionHandler
  adapter/in/event/     <- OrderBatchConsumer (RabbitListener em lote de 200)
  adapter/out/cache/    <- RedisStockCacheAdapter (Lua atômico), RedisIdempotencyAdapter
  adapter/out/messaging/<- RabbitOrderPublisherAdapter, OrderMessage, RabbitTopology
  adapter/out/database/ <- OrderJpaEntity, OrderJpaRepository, OrderPersistenceAdapter
  config/               <- RedisConfig, RabbitConfig, UseCaseConfig, StockSeedRunner
```

## Decisões técnicas já fechadas com o time

1. **Reserva atômica**: script Lua faz `DECR` + `INCR` de compensação numa
   única operação atômica — nunca um `DECR` "nu", que oversell sob concorrência.
2. **Consumo em lote**: `SimpleRabbitListenerContainerFactory` com
   `batchSize=200` e timeout de 5s.
3. **Retry**: 3 tentativas, backoff exponencial 1s → 2s → 4s
   (`RetryTemplate` em `RabbitConfig.batchRetryTemplate`).
4. **Falha definitiva**: após esgotar as tentativas, o worker marca o pedido
   `FAILED` e devolve (`INCR`) o estoque no Redis.
5. **Idempotência**: chave gerada pelo cliente via header `Idempotency-Key`,
   guardada no Redis com `SETNX` + TTL de 24h.

## Testes

- `OrderTest` — regras de transição de estado do agregado.
- `ReserveStockUseCaseImplTest`, `PersistOrderBatchUseCaseImplTest`,
  `GetOrderStatusUseCaseImplTest` — Mockito, cobrindo caminho feliz e bordas.
- `RedisStockCacheAdapterIT` — **Testcontainers**, dispara 500 requisições
  concorrentes contra 100 unidades e prova que exatamente 100 sucedem e o
  contador nunca fica negativo.
- `ReservationControllerTest` — MockMvc, cobrindo 202/409/400.
- `OrderBatchConsumerTest` — prova que o retry esgota e a compensação roda.

```bash
./gradlew test
```
