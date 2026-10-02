(ns trastes.http
  "Camada HTTP: rotas e handlers.

  Segue o mesmo modelo do Ring (padrão de APIs web em Clojure): uma
  requisição é um MAP e a resposta também é um MAP
    requisição -> {:metodo \"GET\" :caminho \"/api/trastes\" :query \"escala=650\"}
    resposta   -> {:status 200 :headers {...} :body \"...\"}
  Assim, os handlers são funções puras e podem ser testados sem subir
  servidor nenhum."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [trastes.calculo :as calculo]
            [trastes.json :as json]
            [trastes.validacao :as validacao])
  (:import (java.net URLDecoder)
           (java.nio.charset StandardCharsets)))

(def versao "1.0.0")

(def cabecalhos-seguranca
  "Cabeçalhos enviados em todas as respostas."
  {"X-Content-Type-Options" "nosniff"
   "Referrer-Policy"        "no-referrer"
   "Cache-Control"          "no-store"})

(defn- origem-cors
  "Origem liberada para chamadas de outros sites (ex.: o site do luthier).
  Configurável pela variável de ambiente CORS_ORIGIN; padrão: qualquer origem."
  []
  (or (System/getenv "CORS_ORIGIN") "*"))

(defn- cabecalhos-cors []
  {"Access-Control-Allow-Origin"  (origem-cors)
   "Access-Control-Allow-Methods" "GET, OPTIONS"
   "Access-Control-Allow-Headers" "Content-Type"})

(defn resposta-json
  [status dados]
  {:status  status
   :headers (merge cabecalhos-seguranca
                   (cabecalhos-cors)
                   {"Content-Type" "application/json; charset=utf-8"})
   :body    (json/->json dados)})

(defn- decodificar [s]
  (URLDecoder/decode ^String s StandardCharsets/UTF_8))

(defn parse-query
  "\"escala=650&trastes=22\" -> {\"escala\" \"650\", \"trastes\" \"22\"}"
  [query]
  (if (str/blank? query)
    {}
    (into {}
          (for [par   (str/split query #"&")
                :let  [[k v] (str/split par #"=" 2)]
                :when (not (str/blank? k))]
            [(decodificar k) (decodificar (or v ""))]))))

;; --------------------------------------------------------------------
;; Handlers
;; --------------------------------------------------------------------

(defn handler-saude [_req]
  (resposta-json 200 {:status "ok" :servico "api-trastes" :versao versao}))

(defn handler-trastes
  "GET /api/trastes?escala=650&trastes=22"
  [req]
  (let [params    (parse-query (:query req))
        resultado (validacao/validar-parametros params)]
    (if-let [erros (:erros resultado)]
      (resposta-json 400 {:erro "Parâmetros inválidos" :detalhes erros})
      (let [{:keys [escala trastes]} (:ok resultado)]
        (resposta-json 200 (calculo/calcular escala trastes))))))

(defn handler-pagina
  "Serve o front-end (resources/public/index.html)."
  [_req]
  (if-let [arquivo (io/resource "public/index.html")]
    {:status  200
     :headers (merge cabecalhos-seguranca
                     {"Content-Type" "text/html; charset=utf-8"
                      "Content-Security-Policy"
                      "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'"})
     :body    (slurp arquivo :encoding "UTF-8")}
    (resposta-json 404 {:erro "Front-end não encontrado"})))

(defn handler-preflight
  "Resposta ao 'preflight' CORS (método OPTIONS) dos navegadores."
  [_req]
  {:status 204 :headers (merge cabecalhos-seguranca (cabecalhos-cors)) :body ""})

;; --------------------------------------------------------------------
;; Roteamento
;; --------------------------------------------------------------------

(def rotas
  "Tabela de rotas: [método caminho] -> handler. É só um map!"
  {["GET" "/"]             handler-pagina
   ["GET" "/index.html"]   handler-pagina
   ["GET" "/api/saude"]    handler-saude
   ["GET" "/api/trastes"]  handler-trastes})

(defn app
  "Função principal da aplicação: recebe uma requisição, devolve uma resposta."
  [{:keys [metodo caminho] :as req}]
  (let [caminho (if (and (> (count caminho) 1) (str/ends-with? caminho "/"))
                  (subs caminho 0 (dec (count caminho)))
                  caminho)]
    (cond
      (= metodo "OPTIONS")
      (handler-preflight req)

      (contains? rotas [metodo caminho])
      ((get rotas [metodo caminho]) req)

      (some #(= caminho (second %)) (keys rotas))
      (resposta-json 405 {:erro (str "Método " metodo " não permitido. Use GET.")})

      :else
      (resposta-json 404 {:erro "Rota não encontrada"
                          :rotas_disponiveis ["GET /api/trastes?escala=650"
                                              "GET /api/saude"
                                              "GET /"]}))))
