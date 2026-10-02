(ns trastes.http-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [trastes.http :as http]
            [trastes.json :as json]))

(defn- get-req [caminho query]
  {:metodo "GET" :caminho caminho :query query})

(deftest calculo-ok
  (let [resp (http/app (get-req "/api/trastes" "escala=650&trastes=3"))]
    (is (= 200 (:status resp)))
    (is (str/starts-with? (get-in resp [:headers "Content-Type"]) "application/json"))
    (is (str/includes? (:body resp) "\"distancia_pestana_mm\":36.48"))
    (is (str/includes? (:body resp) "\"quantidade_trastes\":3"))))

(deftest erros-de-entrada
  (testing "sem escala -> 400"
    (is (= 400 (:status (http/app (get-req "/api/trastes" nil))))))
  (testing "escala inválida -> 400 com mensagem"
    (let [resp (http/app (get-req "/api/trastes" "escala=abc"))]
      (is (= 400 (:status resp)))
      (is (str/includes? (:body resp) "inválid")))))

(deftest rotas
  (is (= 200 (:status (http/app (get-req "/api/saude" nil)))))
  (is (= 200 (:status (http/app (get-req "/api/trastes/" "escala=650")))) "aceita barra final")
  (is (= 404 (:status (http/app (get-req "/nao-existe" nil)))))
  (is (= 405 (:status (http/app {:metodo "POST" :caminho "/api/trastes" :query nil}))))
  (is (= 204 (:status (http/app {:metodo "OPTIONS" :caminho "/api/trastes" :query nil})))))

(deftest parse-query
  (is (= {"escala" "648.5" "trastes" "22"} (http/parse-query "escala=648.5&trastes=22")))
  (is (= {"escala" "648,5"} (http/parse-query "escala=648%2C5")))
  (is (= {} (http/parse-query nil))))

(deftest json
  (is (= "{\"a\":1,\"b\":[true,null,\"x\"]}" (json/->json {:a 1 :b [true nil "x"]})))
  (is (= "\"aspas \\\" e barra \\\\\"" (json/->json "aspas \" e barra \\"))))
