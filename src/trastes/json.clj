(ns trastes.json
  "Conversão de dados Clojure para JSON.

  Implementação mínima e sem dependências externas: suporta maps, vectors,
  listas, strings, keywords, números, booleanos e nil — tudo o que a API
  precisa devolver."
  (:require [clojure.string :as str]))

(defn- escapar
  "Escapa caracteres especiais de uma string para JSON."
  [^String s]
  (let [sb (StringBuilder.)]
    (doseq [c s]
      (case c
        \" (.append sb "\\\"")
        \\ (.append sb "\\\\")
        \newline (.append sb "\\n")
        \return (.append sb "\\r")
        \tab (.append sb "\\t")
        (if (< (int c) 0x20)
          (.append sb (format "\\u%04x" (int c)))
          (.append sb c))))
    (str "\"" sb "\"")))

(defn ->json
  "Converte um valor Clojure em texto JSON."
  [v]
  (cond
    (nil? v)     "null"
    (boolean? v) (str v)
    (number? v)  (if (and (float? v) (not (Double/isFinite (double v))))
                   "null"
                   (str v))
    (keyword? v) (escapar (name v))
    (string? v)  (escapar v)
    (map? v)     (str "{"
                      (str/join "," (map (fn [[k val]]
                                           (str (escapar (if (keyword? k) (name k) (str k)))
                                                ":" (->json val)))
                                         v))
                      "}")
    (sequential? v) (str "[" (str/join "," (map ->json v)) "]")
    :else        (escapar (str v))))
