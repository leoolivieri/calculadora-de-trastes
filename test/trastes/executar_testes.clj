(ns trastes.executar-testes
  "Executa todos os testes: clj -M:test"
  (:require [clojure.test :as t]
            [trastes.calculo-test]
            [trastes.validacao-test]
            [trastes.http-test]))

(defn -main [& _]
  (let [{:keys [fail error]} (t/run-tests 'trastes.calculo-test
                                          'trastes.validacao-test
                                          'trastes.http-test)]
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
