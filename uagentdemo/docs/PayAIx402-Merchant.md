Source: https://docs.payai.network/x402/introduction

# x402 Introduction

> Learn about the x402 protocol

<img src="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=3adb59297326a36c2422e271a45c4ade" alt="x402" data-og-width="1829" width="1829" data-og-height="1418" height="1418" data-path="images/x402/x402-1.webp" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=280&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=2606cd1d2b0ece885fd6b7a8a16fc8b5 280w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=560&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=ad823f5a8515f2c9925cd0c826bb92bf 560w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=840&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=28d4d6e2aae02b3b9590f9a0b2f49f4e 840w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=1100&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4083bc89daceda38164d556130a7219b 1100w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=1650&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=afc436ee76f5d066dd6b6ec44bf768e4 1650w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402/x402-1.webp?w=2500&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=054f453540924c9cdff1bf4a29940b0e 2500w" />

## What is x402?

x402 is an open payment protocol that brings stablecoin payments to plain HTTP.

It revives the `HTTP 402 Payment Required` status so that servers can charge for APIs and digital content seamlessly.

Clients (human users or AI agents) can pay programmatically to access resources without accounts, API keys, or complex authentication.

<Note>Learn more about the x402 project at [x402.org](https://x402.org).</Note>

## Why x402?

Legacy payment rails weren’t built for the web’s speed or for machine-to-machine use.

They are slow, costly, and require sign-ups and keys.

x402 embeds payment into the web’s native request–response flow, enabling instant, global, usage-based payments with minimal integration—ideal for humans and autonomous agents alike.

## Benefits

* Simple HTTP integration using status code 402
* Pay-per-request and other usage-based pricing
* Micropayments with stablecoins (e.g., USDC on Solana)
* Agent-native: AI agents can discover and pay automatically
* Zero friction: no accounts, API keys, or session management

## How it works (high level)

1. A buyer (client) requests a resource from a seller (server).
2. If payment is required, the seller responds with 402 and payment instructions.
3. The buyer constructs and sends a payment payload.
4. The seller verifies and settles the payment (often via a facilitator) and then returns the resource.

<img src="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4386711b36dc1b125e45096054710728" alt="x402 sequence diagram" data-og-width="2056" width="2056" data-og-height="1464" height="1464" data-path="images/x402-sequence-diagram.svg" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=280&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4171f9f1f2813042aec19eb01592746a 280w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=560&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=1b8a92664524acba79b4969a083c8ad3 560w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=840&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=bd2a43e0a1f629ccd84cb079229c1253 840w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=1100&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=31ed2034a4a52ccd5c1ff3ffb93a48e8 1100w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=1650&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4a3c180c15871c8fba1ad5b2876dec4d 1650w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=2500&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=9a5840887509c156ffcd9fa62eb88bdf 2500w" />

## Getting started

<Card title="Quickstart" icon="rocket" horizontal href="/x402/quickstart">
  Quickly get started buying/selling your first service!
</Card>

## Need help?

<Card title="Join our Community" icon="discord" href="https://discord.gg/eWJRwMpebQ">
  Have questions or want to connect with other developers? Join our Discord server.
</Card>


Source: https://docs.payai.network/x402/servers/introduction

# Merchant Introduction

> Learn how to sell services with x402.

## Monetize your API with x402

x402 lets you monetize HTTP APIs and content with on-chain payments, while keeping your existing server stack.

Benefits:

✅ Merchants don't pay network fees.\
✅ Customers don't pay network fees.\
✅ Payment settles in \< 1 second.\
✅ Universal compatibility -- if it speaks HTTP, it speaks x402.

## Architecture at a glance

<img src="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4386711b36dc1b125e45096054710728" alt="x402 sequence diagram" data-og-width="2056" width="2056" data-og-height="1464" height="1464" data-path="images/x402-sequence-diagram.svg" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=280&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4171f9f1f2813042aec19eb01592746a 280w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=560&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=1b8a92664524acba79b4969a083c8ad3 560w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=840&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=bd2a43e0a1f629ccd84cb079229c1253 840w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=1100&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=31ed2034a4a52ccd5c1ff3ffb93a48e8 1100w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=1650&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=4a3c180c15871c8fba1ad5b2876dec4d 1650w, https://mintcdn.com/payai/mg1pvJPl2NNq9wNO/images/x402-sequence-diagram.svg?w=2500&fit=max&auto=format&n=mg1pvJPl2NNq9wNO&q=85&s=9a5840887509c156ffcd9fa62eb88bdf 2500w" />

* **Client**: Calls your protected resource and submits payments.
* **Server (merchant)**: Advertises payment requirements, verifies payments, fulfills requests, and settles payments.
* **Facilitator**: Verifies and/or settles payments on your behalf via standard endpoints.
* **Blockchain**: Where payments are executed and confirmed.

## Getting started

Select one of the quickstart examples, or read the [reference](/x402/reference) for more details.

<Tabs>
  <Tab title="TypeScript">
    <CardGroup>
      <Card title="Express" href="/x402/servers/typescript/express" icon="server">
        Quickstart for building an x402-enabled server with Express.
      </Card>

      <Card title="Hono" href="/x402/servers/typescript/hono" icon="server">
        Quickstart for building an x402-enabled server with Hono.
      </Card>

      <Card title="NextJS (coming soon)" href="/x402/servers/typescript/nextjs" icon="server">
        NextJS server quickstart is coming soon.
      </Card>
    </CardGroup>
  </Tab>

  <Tab title="Python">
    <CardGroup>
      <Card title="FastAPI" href="/x402/servers/python/fastapi" icon="server">
        Quickstart for building an x402-enabled server with FastAPI.
      </Card>

      <Card title="Flask" href="/x402/servers/python/flask" icon="server">
        Quickstart for building an x402-enabled server with Flask.
      </Card>
    </CardGroup>
  </Tab>
</Tabs>

## Facilitator URL

The PayAI facilitator URL is `https://facilitator.payai.network`.

## x402 reference

For a deeper dive into message shapes, headers, verification and settlement responses, see the [x402 Reference](/x402/reference).

## Need help?

<Card title="Join our Community" icon="discord" href="https://discord.gg/eWJRwMpebQ">
  Have questions or want to connect with other developers? Join our Discord server.
</Card>
