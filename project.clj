(defproject chat "0.1.0-SNAPSHOT"

  :description "a simple chat"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.76"]
                 [selmer "1.0.7"]
                 [markdown-clj "0.9.89"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.8.0"]
                 [bouncer "1.0.0"]
                 [reagent-utils "0.1.9"]
                 [reagent "0.6.0-rc"]
                 [org.webjars/bootstrap "4.0.0-alpha.2"]
                 [org.webjars/font-awesome "4.6.3"]
                 [org.webjars.bower/tether "1.3.2"]
                 [org.webjars/jquery "2.2.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.5.1"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-servlet "1.4.0"]
                 [mount "0.1.10"]
                 [cprop "0.1.8"]
                 [org.clojure/tools.cli "0.3.5"]
                 [luminus-nrepl "0.1.4"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [luminus-immutant "0.2.2"]
                 [cheshire "5.5.0"]
                 [javax/javaee-web-api "7.0" :scope "provided"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main chat.core

  :plugins [[lein-cprop "1.0.1"]
            [lein-immutant "2.1.0"]
            [lein-uberwar "0.2.0"]
            [lein-cljsbuild "1.1.3"]]

  :uberwar {:handler chat.handler/app
            :init chat.handler/init
            :destroy chat.handler/destroy
            :name "chat.war"}
  :aliases {"uberwar" ["update-in" ":" "assoc" ":source-paths"
                       "[\"src/clj\" \"war/clj\" \"env/prod/clj\"]"
                       "--" "uberwar"]
            "repl" ["update-in" ":" "assoc" ":source-paths"
                    "[\"src/clj\" \"jar/clj\" \"env/dev/clj\"]"
                    "--" "repl"]
            "uberjar" ["update-in" ":" "assoc" ":source-paths"
                       "[\"src/clj\" \"jar/clj\" \"env/prod/clj\"]"
                       "--" "uberjar"]}

  
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src/cljs" "env/dev/cljs"]
     :figwheel true
     :compiler
     {:main  "chat.app"
      :asset-path "js/out"
      :output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :optimizations :none
      :source-map true
      :pretty-print true}}
    :min
    {:source-paths ["src/cljs" "env/prod/cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/uberjar"
      :externs ["react/externs/react.js"]
      :optimizations :advanced
      :pretty-print false}}}}

  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :aot :all
             :uberjar-name "chat.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.1"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.5.0"]
                                 [pjstadig/humane-test-output "0.8.0"]
                                 [binaryage/devtools "0.7.0"]
                                 [figwheel-sidecar "0.5.4-3"]
                                 [com.cemerick/piggieback "0.2.2-SNAPSHOT"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.14.0"]
                                 [lein-figwheel "0.5.4-3"]
                                 [org.clojure/clojurescript "1.9.76"]]
                  
                  :source-paths ["env/dev/clj" "test/clj" "jar/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/dev/resources" "env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
