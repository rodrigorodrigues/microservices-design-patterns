(ns clojure-service.core-test
  (:require [clojure.test :refer :all]
            [clojure-service.core :refer :all]))

(deftest test-placeholder
  (testing "Placeholder test"
    (is (= 1 1))))
