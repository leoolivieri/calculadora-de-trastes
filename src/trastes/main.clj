(ns trastes.main
  "Ponto de entrada da aplicação: clj -M:run"
  (:require [trastes.servidor :as servidor])
  (:gen-class))

(defn- porta []
  (let [p (System/getenv "PORT")]
    (if (and p (re-matches #"\d{2,5}" p))
      (Integer/parseInt p)
      3000)))

(defn -main [& _args]
  (let [p (porta)]
    (servidor/iniciar! p)
    (println "==============================================")
    (println " API Calculadora de Trastes rodando!")
    (println (str " Front-end: http://localhost:" p "/"))
    (println (str " API:       http://localhost:" p "/api/trastes?escala=650"))
    (println " Para encerrar: Ctrl+C")
    (println "==============================================")
    ;; Mantém o processo vivo enquanto o servidor atende requisições.
    @(promise)))
