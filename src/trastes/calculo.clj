(ns trastes.calculo
  "Cálculo da posição dos trastes em instrumentos de afinação temperada.

  Todas as funções deste namespace são PURAS: recebem valores e retornam
  valores, sem efeitos colaterais (sem I/O, sem estado). Isso facilita os
  testes e é o coração funcional do projeto.

  Fórmula (temperamento igual, 12 semitons por oitava):
    d(n) = L * (1 - 1 / 2^(n/12))
  onde L = escala em mm e n = número do traste.")

(def formula
  "Representação textual da fórmula, devolvida pela API."
  "d_n = L * (1 - 1 / 2^(n/12))")

(defn distancia-pestana
  "Distância (mm) da pestana até o traste n, para a escala L (mm)."
  [L n]
  (* L (- 1.0 (/ 1.0 (Math/pow 2.0 (/ n 12.0))))))

(defn distancia-ponte
  "Distância (mm) do traste n até a ponte: L - d(n)."
  [L n]
  (- L (distancia-pestana L n)))

(defn tamanho-espaco
  "Tamanho do espaço do traste n:
   - traste 1:  d(1)
   - demais:    d(n) - d(n-1)"
  [L n]
  (if (= n 1)
    (distancia-pestana L 1)
    (- (distancia-pestana L n) (distancia-pestana L (dec n)))))

(defn arredonda
  "Arredonda x para 2 casas decimais."
  [x]
  (/ (Math/round (* x 100.0)) 100.0))

(defn traste
  "Map com todos os dados de um traste (valores já arredondados)."
  [L n]
  {:traste               n
   :tamanho_espaco_mm    (arredonda (tamanho-espaco L n))
   :distancia_ponte_mm   (arredonda (distancia-ponte L n))
   :distancia_pestana_mm (arredonda (distancia-pestana L n))})

(defn tabela-trastes
  "Vector com os dados dos trastes 1..quantidade para a escala L."
  [L quantidade]
  (mapv #(traste L %) (range 1 (inc quantidade))))

(defn verificacao-oitava
  "Verificação pedida na especificação: no 12º traste a distância até a
  pestana deve ser exatamente metade da escala."
  [L]
  (let [d12 (distancia-pestana L 12)]
    {:traste_12_distancia_pestana_mm (arredonda d12)
     :metade_da_escala_mm            (arredonda (/ L 2.0))
     :confere                        (< (Math/abs (- d12 (/ L 2.0))) 1e-9)}))

(defn calcular
  "Resultado completo do cálculo, no formato devolvido pela API."
  [L quantidade]
  {:escala_mm          (arredonda L)
   :quantidade_trastes quantidade
   :formula            formula
   :verificacao        (verificacao-oitava L)
   :trastes            (tabela-trastes L quantidade)})
