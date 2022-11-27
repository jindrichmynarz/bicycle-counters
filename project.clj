(defproject cz.auto-mat.bicycle-counters "0.1.0-SNAPSHOT"
  :description "Collecting Prague bicycle counters data for analyses by Auto-mat.cz"
  :url "https://github.com/jindrichmynarz/bicycle-counters"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [com.github.seancorfield/next.jdbc "1.2.780"]
                 [com.layerware/hugsql "0.5.3"]
                 [com.zaxxer/HikariCP "5.0.1"]
                 [environ "1.2.0"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "1.0.1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.postgresql/postgresql "42.3.6"]
                 [org.slf4j/slf4j-nop "1.8.0-beta4"]
                 [slingshot "0.12.2"]
                 [com.taoensso/timbre "5.2.1"]]
  :main cz.auto-mat.bicycle-counters.cli
  :profiles {:dev {:resource-paths ["test/resources"]}
             :uberjar {:aot :all
                       :omit-source true
                       :uberjar-name "bicycle_counters.jar"}}
  :repl-options {:init-ns cz.auto-mat.bicycle-counters.core})
