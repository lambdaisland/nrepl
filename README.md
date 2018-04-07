# lambdaisland/nrepl

Main namespace for starting an nREPL server with `clj`

```
clj -m lambdaisland.nrepl.main
```

## Version information

``` clojure
{lambdaisland/nrepl {:mvn/version "0.2.0}}
```

## Rationale

Provide a convenient way to start an nREPL server from the command line with
`clj`, that is on par with how Leiningen or Boot does it.

There are some things that Leiningen does, that were later copied to Boot, such
as having a configurable intial namespace that gets loaded (defaults to `user`),
and making sure `*data-readers*` are propagated to the REPL thread. Extract
these features in a minimal library so people don't have to keep copying or
reinventing these.

When using nREPL directly it asks you for a handler. Using a custom handler
implementation is rare, using custom middleware is common, therefore always use
the default handler, but make it easy to configure middleware.

Only start an nREPL server, don't provide an actual REPL, since `clojure.main`
already does that.

Create an `.nrepl-port` file in the current directory, so tooling can discover
the instance.

## Usage

```
$ clj -m lambdaisland.nrepl.main --nrepl-help

USAGE:

clj -m lambdaisland.nrepl.main [OPTIONS]

  -p, --port PORT          Port to run nREPL on
  -i, --init-ns SYMBOL     Initial namespace, defaults to `user`
  -m, --middleware SYMBOL  Extra middleware to inject. Can be specified multiple times. Use a namespaced symbol.
  -b, --bind               Host to bind the interface on. Default 127.0.0.1
  -s, --silent             Don't print the welcome message
  -H, --nrepl-help         Display this help message
```

Full example

```
$ clj -m lambdaisland.nrepl.main \
  --init-ns project.repl \
  --port 9999 \
  --bind 0.0.0.0 \
  --middleware cider.nrepl/cider-middleware \
  --middleware refactor-nrepl.middleware/wrap-refactor \
  --middleware cemerick.piggieback/wrap-cljs-repl

Starting nREPL server on 0.0.0.0:9999
```

## One-liner

The recommended way to use `lambdaisland/nrepl` is by adding it to your `deps.edn`, but you can also invoke it directly like this:

```
clj -Sdeps '{:deps {lambdaisland/nrepl {:mvn/version "VERSION"}}}' -m lambdaisland.nrepl.main
```

## Use as a library

You can consume `lambdaisland/nrepl` directly. The options map corresponds
directly with the command line options.

``` clojure
(require 'lambdaisland.nrepl)

;; use defaults, port 7888
(lambdaisland.nrepl/start-server {})

;; takes the same options as the command line version
(lambdaisland.nrepl/start-server
  {:port 9999
   :bind "0.0.0.0"
   :middleware '[cider.nrepl/cider-middleware]
   :init-ns 'project.repl})
```

## License

Copyright &copy; 2018 Arne Brasseur

Distributed under the Eclipse Public License v1.0. See LICENSE.txt.
