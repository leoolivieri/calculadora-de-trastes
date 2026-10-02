(ns trastes.calculo-test
  (:require [clojure.test :refer [deftest is testing]]
            [trastes.calculo :as c]))

(deftest exemplo-da-especificacao-650mm
  (testing "Valores do slide 'Exemplo' (escala 650 mm)"
    (is (= {:traste 1 :tamanho_espaco_mm 36.48 :distancia_ponte_mm 613.52 :distancia_pestana_mm 36.48}
           (c/traste 650 1)))
    (is (= {:traste 2 :tamanho_espaco_mm 34.43 :distancia_ponte_mm 579.08 :distancia_pestana_mm 70.92}
           (c/traste 650 2)))
    (is (= {:traste 3 :tamanho_espaco_mm 32.5 :distancia_ponte_mm 546.58 :distancia_pestana_mm 103.42}
           (c/traste 650 3)))))

(deftest verificacao-do-12-traste
  (testing "No 12º traste a distância até a pestana é metade da escala"
    (is (= 325.0 (c/arredonda (c/distancia-pestana 650 12))))
    (is (= 325.0 (c/arredonda (c/distancia-ponte 650 12))))
    (is (true? (:confere (c/verificacao-oitava 650))))
    (is (true? (:confere (c/verificacao-oitava 628.65))))))

(deftest planilha-de-referencia-655mm
  (testing "Valores da planilha do luthier (escala 655 mm)"
    (let [t (c/tabela-trastes 655 22)]
      (is (= 22 (count t)))
      (is (= {:traste 1 :tamanho_espaco_mm 36.76 :distancia_ponte_mm 618.24 :distancia_pestana_mm 36.76}
             (first t)))
      (is (= {:traste 12 :tamanho_espaco_mm 19.47 :distancia_ponte_mm 327.5 :distancia_pestana_mm 327.5}
             (nth t 11)))
      (is (= {:traste 22 :tamanho_espaco_mm 10.93 :distancia_ponte_mm 183.8 :distancia_pestana_mm 471.2}
             (last t))))))

(deftest propriedades-gerais
  (let [t (c/tabela-trastes 650 24)]
    (testing "Os espaços diminuem a cada traste (progressão exponencial)"
      (is (apply > (map :tamanho_espaco_mm t))))
    (testing "Pestana + ponte = escala, para todo traste"
      (is (every? #(< (Math/abs (- 650 (+ (:distancia_pestana_mm %) (:distancia_ponte_mm %)))) 0.011) t)))
    (testing "No 24º traste (2 oitavas) a distância à pestana é 3/4 da escala"
      (is (= 487.5 (:distancia_pestana_mm (last t)))))))
