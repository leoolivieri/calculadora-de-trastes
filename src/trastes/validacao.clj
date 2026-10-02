(ns trastes.validacao
  "Validação dos dados de entrada.

  Regra geral: nunca confiar na entrada do usuário. Cada parâmetro é
  convertido e conferido contra limites explícitos; qualquer problema vira
  uma mensagem de erro clara (HTTP 400), nunca uma exceção sem tratamento."
  (:require [clojure.string :as str]))

(def limites
  {:escala-min     100.0   ; mm — menor que um cavaquinho/ukulele sopranino
   :escala-max     1200.0  ; mm — acima de qualquer contrabaixo elétrico
   :trastes-min    1
   :trastes-max    36
   :trastes-padrao 22      ; mesma quantidade da planilha de referência
   :tamanho-max    20})    ; máximo de caracteres aceitos por parâmetro

(defn- texto->numero
  "Converte texto em double. Aceita vírgula decimal (\"648,5\").
  Retorna nil se o texto não for um número finito."
  [s]
  (when (and (string? s)
             (<= (count s) (:tamanho-max limites))
             (re-matches #"\s*-?\d+([.,]\d+)?\s*" s))
    (let [n (Double/parseDouble (str/replace (str/trim s) "," "."))]
      (when (Double/isFinite n) n))))

(defn- texto->inteiro
  "Converte texto em inteiro (long). Retorna nil se inválido."
  [s]
  (when (and (string? s)
             (<= (count s) (:tamanho-max limites))
             (re-matches #"\s*-?\d+\s*" s))
    (Long/parseLong (str/trim s))))

(defn validar-escala
  "Retorna {:ok escala} ou {:erro mensagem}."
  [s]
  (let [{:keys [escala-min escala-max]} limites]
    (cond
      (str/blank? s)
      {:erro "Parâmetro 'escala' é obrigatório (em mm). Exemplo: ?escala=650"}

      (nil? (texto->numero s))
      {:erro (str "Parâmetro 'escala' inválido: '" (subs s 0 (min (count s) 20))
                  "'. Informe um número, por exemplo 650 ou 648.5")}

      (not (<= escala-min (texto->numero s) escala-max))
      {:erro (format "A escala deve estar entre %.0f e %.0f mm." escala-min escala-max)}

      :else
      {:ok (texto->numero s)})))

(defn validar-trastes
  "Quantidade de trastes é opcional (padrão 22). Retorna {:ok n} ou {:erro msg}."
  [s]
  (let [{:keys [trastes-min trastes-max trastes-padrao]} limites]
    (cond
      (str/blank? s)
      {:ok trastes-padrao}

      (nil? (texto->inteiro s))
      {:erro "Parâmetro 'trastes' inválido: informe um número inteiro, por exemplo 22."}

      (not (<= trastes-min (texto->inteiro s) trastes-max))
      {:erro (format "A quantidade de trastes deve estar entre %d e %d." trastes-min trastes-max)}

      :else
      {:ok (texto->inteiro s)})))

(defn validar-parametros
  "Valida os parâmetros da requisição (map de strings).
  Retorna {:ok {:escala .. :trastes ..}} ou {:erros [mensagens]}."
  [params]
  (let [escala  (validar-escala (get params "escala"))
        trastes (validar-trastes (get params "trastes"))
        erros   (keep :erro [escala trastes])]
    (if (seq erros)
      {:erros (vec erros)}
      {:ok {:escala (:ok escala) :trastes (:ok trastes)}})))
