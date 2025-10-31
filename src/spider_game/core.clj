(ns spider-game.core
  (:gen-class)
  (:require [clunk.core :as c]
            [spider-game.scenes.level-01 :as level-01]))

(defn init-scenes
  "Map of scenes in the game"
  [state]
  {:level-01 (level-01/init state)})

;; Configure the game
(def spider-game-game
  (c/game {:title "spider-game"
           :size [800 600]
           :init-scenes-fn init-scenes
           :current-scene :level-01
           :assets {:audio {:step-1 "resources/audio/step-1.ogg"
                            :step-2 "resources/audio/step-2.ogg"
                            :step-3 "resources/audio/step-3.ogg"
                            :step-4 "resources/audio/step-4.ogg"
                            :step-5 "resources/audio/step-5.ogg"}}}))

(defn -main
  "Run the game"
  [& args]
  (c/start! spider-game-game))
