(ns reagent-test-table.prod
  (:require [reagent-test-table.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
