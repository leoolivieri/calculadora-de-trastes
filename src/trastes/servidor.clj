(ns trastes.servidor
  "Adaptador HTTP: liga a função `app` (trastes.http) ao servidor HTTP que já
  vem embutido no Java (com.sun.net.httpserver). Por isso o projeto não
  depende de nenhuma biblioteca externa."
  (:require [trastes.http :as http])
  (:import (com.sun.net.httpserver HttpServer HttpHandler HttpExchange)
           (java.net InetSocketAddress)
           (java.nio.charset StandardCharsets)
           (java.util.concurrent Executors)))

(defn- exchange->requisicao
  "Converte o objeto HttpExchange do Java em um map Clojure."
  [^HttpExchange ex]
  {:metodo  (.getRequestMethod ex)
   :caminho (.getPath (.getRequestURI ex))
   :query   (.getRawQuery (.getRequestURI ex))})

(defn- enviar-resposta!
  "Escreve o map de resposta no HttpExchange."
  [^HttpExchange ex {:keys [status headers body]}]
  (let [bytes (.getBytes ^String (or body "") StandardCharsets/UTF_8)
        hdrs  (.getResponseHeaders ex)]
    (doseq [[k v] headers]
      (.set hdrs k v))
    (if (zero? (alength bytes))
      (.sendResponseHeaders ex status -1)
      (do (.sendResponseHeaders ex status (alength bytes))
          (with-open [out (.getResponseBody ex)]
            (.write out bytes))))))

(defn- log [req resp]
  (println (format "%s %s%s -> %d"
                   (:metodo req) (:caminho req)
                   (if (:query req) (str "?" (:query req)) "")
                   (:status resp))))

(defn- handler-java
  "Cria o HttpHandler do Java que chama a função `app`."
  [app]
  (reify HttpHandler
    (handle [_ ex]
      (try
        (let [req  (exchange->requisicao ex)
              resp (try
                     (app req)
                     (catch Exception e
                       ;; Erro inesperado: registra no console, mas não expõe
                       ;; detalhes internos (stack trace) para o cliente.
                       (binding [*out* *err*]
                         (println "Erro interno:" (.getMessage e)))
                       (http/resposta-json 500 {:erro "Erro interno do servidor"})))]
          (log req resp)
          (enviar-resposta! ex resp))
        (finally
          (.close ex))))))

(defn iniciar!
  "Sobe o servidor na porta informada e retorna o objeto HttpServer."
  [porta]
  (let [servidor (HttpServer/create (InetSocketAddress. (int porta)) 0)]
    (.createContext servidor "/" (handler-java http/app))
    (.setExecutor servidor (Executors/newFixedThreadPool 8))
    (.start servidor)
    servidor))

(defn parar! [^HttpServer servidor]
  (.stop servidor 0))
