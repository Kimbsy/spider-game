(ns spider-game.core
  (:gen-class)
  (:require [clunk.core :as c]
            [spider-game.scenes.bite-overlay :as bite-overlay]
            [spider-game.scenes.level-01 :as level-01]
            [spider-game.scenes.menu :as menu]
            [spider-game.scenes.wrap-overlay :as wrap-overlay]
            [spider-game.scenes.repair-overlay :as repair-overlay]
            [spider-game.scenes.round-end :as round-end]))

(defn init-scenes
  "Map of scenes in the game"
  [state]
  {:level-01 (level-01/init state)
   :bite-overlay (bite-overlay/init state)
   :wrap-overlay (wrap-overlay/init state)
   :repair-overlay (repair-overlay/init state)
   :round-end (round-end/init state)
   :menu (menu/init state)})

(def initial-score
  {:flies-caught 0
   :perfect-bites 0
   :spider-happiness "N/A"})

(defn restart
  [state]
  (-> state
      (assoc :score initial-score)
      (assoc :scenes (init-scenes state))))

;; Configure the game
(def spider-game-game
  (c/game {:title "spider-game"
           :size [1000 800]
           :init-scenes-fn init-scenes
           :current-scene :menu
           :assets {:image {:fly-spritesheet "resources/img/fly-spritesheet.png"}
                    :audio {:step-1 "resources/audio/step-1.ogg"
                            :step-2 "resources/audio/step-2.ogg"
                            :step-3 "resources/audio/step-3.ogg"
                            :step-4 "resources/audio/step-4.ogg"
                            :step-5 "resources/audio/step-5.ogg"
                            :munch "resources/audio/munch.ogg"}}
           :score initial-score
           :restart-fn restart}))

(defn -main
  "Run the game"
  [& args]
  (c/start! spider-game-game))
