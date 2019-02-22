(ns status-im.utils.ethereum.tribute
  (:require [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.ethereum.abi-spec :as abi-spec]))

(def contracts
  {:mainnet nil
   :testnet "0x82694E3DeabE4D6f4e6C180Fe6ad646aB8EF53ae"
   :rinkeby nil})

(def contract "0x0000000000000000000000000000000000000000")

(defn set-tribute [web3 contract public-key snt-amount]
  (ethereum/call web3
                 (ethereum/call-params contract "setRequiredTribute(uint256)" snt-amount)
                 (fn [_ count])))
gt
(defn get-tribute [web3 contract public-key cb]
  (ethereum/call web3
                 (ethereum/call-params contract "getFee(address)" public-key)
                 (fn [_ count] (cb (ethereum/hex->int count)))))
