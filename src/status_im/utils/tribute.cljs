(ns status-im.utils.tribute
  (:require [status-im.i18n :as i18n]))

(defn status [[status value]]
  (cond (= status :paid)
        (i18n/label :t/tribute-state-paid)
        (= status :pending)
        (i18n/label :t/tribute-state-pending)
        (= status :required)
        (i18n/label :t/tribute-state-required {:snt-amount value})
        :else nil))

