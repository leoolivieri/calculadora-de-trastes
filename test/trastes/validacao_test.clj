(ns trastes.validacao-test
  (:require [clojure.test :refer [deftest is testing]]
            [trastes.validacao :as v]))

(deftest escala-valida
  (is (= {:ok 650.0} (v/validar-escala "650")))
  (is (= {:ok 648.5} (v/validar-escala "648.5")))
  (testing "aceita vírgula decimal"
    (is (= {:ok 648.5} (v/validar-escala "648,5")))))

(deftest escala-invalida
  (testing "ausente"
    (is (:erro (v/validar-escala nil)))
    (is (:erro (v/validar-escala ""))))
  (testing "não numérica ou com lixo"
    (doseq [s ["abc" "650mm" "6.5.0" "1e3" "NaN" "Infinity" "650; DROP TABLE" "<script>"]]
      (is (:erro (v/validar-escala s)) s)))
  (testing "fora dos limites"
    (doseq [s ["0" "-650" "99" "1201" "99999999"]]
      (is (:erro (v/validar-escala s)) s)))
  (testing "texto muito longo"
    (is (:erro (v/validar-escala (apply str (repeat 500 "9")))))))

(deftest quantidade-de-trastes
  (is (= {:ok 22} (v/validar-trastes nil)) "padrão é 22")
  (is (= {:ok 24} (v/validar-trastes "24")))
  (doseq [s ["0" "37" "-1" "12.5" "vinte"]]
    (is (:erro (v/validar-trastes s)) s)))

(deftest validar-parametros
  (is (= {:ok {:escala 650.0 :trastes 22}} (v/validar-parametros {"escala" "650"})))
  (testing "reporta todos os erros de uma vez"
    (is (= 2 (count (:erros (v/validar-parametros {"escala" "x" "trastes" "y"})))))))
