(ns spider-game.core
  (:gen-class)
  (:require [clunk.audio :as audio]
            [clunk.core :as c]
            [spider-game.scenes.bite-overlay :as bite-overlay]
            [spider-game.scenes.level-01 :as level-01]
            [spider-game.scenes.menu :as menu]
            [spider-game.scenes.repair-overlay :as repair-overlay]
            [spider-game.scenes.round-end :as round-end]
            [spider-game.scenes.wrap-overlay :as wrap-overlay]))

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
                    :audio {:music "resources/audio/music/catching-flies.ogg"
                            :step-1 "resources/audio/sfx/step-1.ogg"
                            :step-2 "resources/audio/sfx/step-2.ogg"
                            :step-3 "resources/audio/sfx/step-3.ogg"
                            :step-4 "resources/audio/sfx/step-4.ogg"
                            :step-5 "resources/audio/sfx/step-5.ogg"
                            }}
           :on-start-fn (fn [state]
                          (assoc state :music-clip
                                 (audio/play! :music :loop? true)))
           :score initial-score
           :restart-fn restart}))

(defn -main
  "Run the game"
  [& args]
  (c/start! spider-game-game))
