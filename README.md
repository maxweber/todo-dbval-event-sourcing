# Todo example that uses dbval and event sourcing

An experiment to see how [dbval](https://github.com/maxweber/dbval) and event
sourcing can be combined, here to realize a basic todo app.

Mostly written by Claude Code (Opus 4.5)

## Why

I couldn't decide between a Datomic-like database and event sourcing, so I
combined them 😄

What I want is a database as a value, with Datalog and something like the
Datomic entity API. However, after using Datomic for almost a decade, I've
noticed that you end up facing challenges similar to those in relational
databases: if your schema design is wrong, you need migrations. Tools like
[schema-migration](https://github.com/simplemono/schema-migration) help, but a
lot of migrations are scary to perform on a production database. If a migration
did something wrong you need to write another one to fix it.

I spent a lot of time thinking about why event sourcing feels different here.
The best explanation I’ve found is that we tend to mix essential state with
derived state, both in relational databases and in Datomic. Imagine an Excel
spreadsheet where cells containing formulas do not update automatically, and
worse, they store the computed result instead of the formula itself. It's
immediately obvious that this is something you want to avoid.

This example combines a Datomic-like database library (dbval) with transactional
event sourcing. The former serves as the read-model, while the events themselves
are immutable values, stored forever. If you later discover that your read-model
was derived incorrectly, you can simply delete it and replay all events to
rebuild it, atomically, in a single transaction.

Another area where event sourcing shines is that it forces you to assign a
meaning to an event. In contrast, transactions in relational databases, or even
in Datomic, can be fairly arbitrary. Datomic transactions at least allow you to
capture the "why", but events go one step further. External event streams, such
as those from [a billing
provider](https://developer.paddle.com/api-reference/events/list-events), make
this especially clear: you can build your own read-model and keep it up to date
simply by applying new events as they arrive.

## Development

Run:

    bin/dev-start

## Licence

MIT
