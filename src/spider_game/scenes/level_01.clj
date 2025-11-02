(ns spider-game.scenes.level-01
  (:require [clunk.collision :as collision]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.common :as common]
            [spider-game.scenes.bite-overlay :as bite-overlay]
            [spider-game.sprites.fly :as fly]
            [spider-game.sprites.player-spider :as ps]
            [spider-game.sprites.web :as web]
            [clunk.shape :as shape]
            [clunk.palette :as p]))

(defn flies
  [n window]
  (repeatedly n #(fly/fly (u/window-pos window [(rand) (rand)]))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  (concat 
   [(web/web window)]
   (flies 10 window)
   [(ps/spider (u/center window))]))

(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! common/dark-green)
  (sprite/draw-scene-sprites! state))

(defn handle-escapes
  [{:keys [current-scene] :as state}]
  (let [flies (filter (sprite/has-group :fly)
                      (get-in state [:scenes current-scene :sprites]))]
    (reduce (fn [acc-state f]
              (if (and (not (pos? (:remaining f)))
                       (not= :escaped (:status f)))
                (-> acc-state
                    (sprite/update-sprites
                     (sprite/is-sprite f)
                     #(assoc % :status :escaped))
                    (sprite/update-sprites
                     (sprite/has-group :web)
                     (fn [w]
                       (web/break-web-at acc-state w (:pos f)))))
                acc-state))
            state
            flies)))

(defn remove-escaped
  [{:keys [current-scene] :as state}]
  (update-in state [:scenes current-scene :sprites]
             (fn [sprites]
               (remove (fn [s]
                         (and ((sprite/has-group :fly) s)
                              (= :escaped (:status s))))
                       sprites))))

(defn update-level-01
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      handle-escapes
      remove-escaped
      sprite/update-state
      tween/update-state
      ((fn [state]
         (sprite/update-sprites
          state
          (sprite/has-group :player-spider)
          (fn [s]
            (assoc s :debug? false)))))
      collision/update-state
      ))

;; @TODO: not working, could do with a "no collisions fn", otherwise we trigger non-hit for each fly that doesn't collide
(defn colliders
  []
  []
  #_[(collision/collider
    :player-spider
    :fly
    (fn [spider _fly]
      (assoc spider :debug? true))
    collision/identity-collide-fn)])

;; @TODO: helpful debugging
(defn break-web-on-click
  [state {:keys [pos] :as e}]
  (if (i/is e i/M_LEFT)
    (sprite/update-sprites
     state
     (sprite/has-group :web)
     (fn [w]
       (web/break-web-at state w pos)))
    state))

(defn clicked
  [state {:keys [pos] :as e}]  
  (sprite/update-sprites
   state
   (sprite/has-group :player-spider)
   #(ps/move % pos)))

(defn bite
  [{:keys [current-scene] :as state} e]
  ;; @TODO: temp for ease of testing
  (cond
    (i/is e :key i/K_SPACE)
    (let [sprites (get-in state [:scenes current-scene :sprites])
          spider (first (filter (sprite/has-group :player-spider) sprites))
          flies (filter (sprite/has-group :fly) sprites)
          target-fly (first (filter #(and (= :escaping (:current-animation %1))
                                          (collision/w-h-rects-collide? %1 spider))
                                    flies))]
      (if target-fly
        (scene/transition
         state
         :bite-overlay
         :transition-length 0
         ;; ensure the goal is in a new random position
         :init-fn (fn [state]
                    (assoc-in state
                              [:scenes :bite-overlay]
                              ;; also insert he fly uuid so we know
                              ;; which sprite to wrap once we're done
                              (assoc (bite-overlay/init state)
                                     :fly-uuid (:uuid target-fly)))))
        state))

    :else
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-level-01!
   :update-fn update-level-01
   :colliders (colliders)
   :mouse-button-fns [clicked]
   :key-fns [bite]})
