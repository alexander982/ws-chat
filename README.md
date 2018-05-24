# chat

generated using Luminus version "2.9.10.79"

A simple websocket chat

## Features

- Frontend and backend in clojure
- History on client side using IndexedDB

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

Before starting web server you need to compile clojurescript

    lein cljsbuild once

To start a web server for the application, run:

    lein run

To start from repl, first run:

    lein figwheel
    
Then in ather console type:

    lein repl

When repl is started, type:
	
    (start)

