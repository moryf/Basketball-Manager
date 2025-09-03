(ns basket-sim.core
  (:gen-class))

;; Helper function to add the initial box score to a player
(defn with-initial-box-score [player]
  (assoc player :box-score {:pts 0, :ast 0, :reb 0, :fga 0, :fgm 0, :3pa 0, :3pm 0}))

;; Nuggets
(def team-a
  {:name "Denver Nuggets"
   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
   {
     :pg {:name "Jamal Murray"
          :usage-rate 27.5, :shot-dist-2p 0.655, :shot-dist-3p 0.345,
          :fg-perc-2p 0.523, :fg-perc-3p 0.425, :orb-perc 2.1, :drb-perc 12.5,
          :ast-perc-2p 0.45, :ast-perc-3p 0.65, :ast-perc 30.1}
     :sg {:name "Kentavious Caldwell-Pope"
          :usage-rate 12.8, :shot-dist-2p 0.435, :shot-dist-3p 0.565,
          :fg-perc-2p 0.528, :fg-perc-3p 0.406, :orb-perc 1.9, :drb-perc 7.8,
          :ast-perc-2p 0.70, :ast-perc-3p 0.95, :ast-perc 10.1}
     :sf {:name "Michael Porter Jr."
          :usage-rate 20.3, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
          :fg-perc-2p 0.596, :fg-perc-3p 0.397, :orb-perc 5.8, :drb-perc 16.6,
          :ast-perc-2p 0.68, :ast-perc-3p 0.90, :ast-perc 6.9}
     :pf {:name "Aaron Gordon"
          :usage-rate 17.8, :shot-dist-2p 0.887, :shot-dist-3p 0.113,
          :fg-perc-2p 0.612, :fg-perc-3p 0.290, :orb-perc 8.4, :drb-perc 15.0,
          :ast-perc-2p 0.65, :ast-perc-3p 0.98, :ast-perc 14.5}
     :c  {:name "Nikola Jokic"
          :usage-rate 29.8, :shot-dist-2p 0.826, :shot-dist-3p 0.174,
          :fg-perc-2p 0.643, :fg-perc-3p 0.359, :orb-perc 10.9, :drb-perc 31.8,
          :ast-perc-2p 0.55, :ast-perc-3p 0.85, :ast-perc 42.3}}))})

;; Warriors
(def team-b
  {:name "Golden State Warriors"
   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
   {
     :pg {:name "Stephen Curry"
          :usage-rate 29.2, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
          :fg-perc-2p 0.518, :fg-perc-3p 0.408, :orb-perc 2.0, :drb-perc 12.1,
          :ast-perc-2p 0.50, :ast-perc-3p 0.75, :ast-perc 25.4}
     :sg {:name "Klay Thompson"
          :usage-rate 22.9, :shot-dist-2p 0.383, :shot-dist-3p 0.617,
          :fg-perc-2p 0.493, :fg-perc-3p 0.387, :orb-perc 1.7, :drb-perc 8.7,
          :ast-perc-2p 0.72, :ast-perc-3p 0.96, :ast-perc 13.1}
     :sf {:name "Andrew Wiggins"
          :usage-rate 18.2, :shot-dist-2p 0.662, :shot-dist-3p 0.338,
          :fg-perc-2p 0.505, :fg-perc-3p 0.358, :orb-perc 5.2, :drb-perc 11.2,
          :ast-perc-2p 0.60, :ast-perc-3p 0.97, :ast-perc 8.4}
     :pf {:name "Draymond Green"
          :usage-rate 13.5, :shot-dist-2p 0.685, :shot-dist-3p 0.315,
          :fg-perc-2p 0.547, :fg-perc-3p 0.395, :orb-perc 3.7, :drb-perc 21.0,
          :ast-perc-2p 0.65, :ast-perc-3p 0.99, :ast-perc 28.6}
     :c  {:name "Kevon Looney"
          :usage-rate 10.1, :shot-dist-2p 0.992, :shot-dist-3p 0.008,
          :fg-perc-2p 0.598, :fg-perc-3p 0.000, :orb-perc 14.1, :drb-perc 21.4,
          :ast-perc-2p 0.80, :ast-perc-3p 1.0, :ast-perc 12.5}}))})

;; Select finisher function based on their usage rate
(defn select-finisher
  "Selects players randomly based on their usage rate"
  [players]
  (let [total-usage (reduce + (map :usage-rate players))]
    (loop [p (rand total-usage)
           [current-player & rest-players] players]
      (if (< p (:usage-rate current-player))
        current-player
        (recur (- p (:usage-rate current-player)) rest-players)))))

;;Shot simualtion function
(defn simulate-shot
  "Simulates shot. a Chooses whether the shot is 2p or 3p based on their tendencies. Hit or miss based on shot percentage"
  [player]
  (let [shot-type (if (< (rand) (:shot-dist-3p player)) :3p :2p)
        shot-made? (if (= shot-type :3p)
                     (< (rand) (:fg-perc-3p player))
                     (< (rand) (:fg-perc-2p player)))]
    {:shot-type shot-type :shot-made? shot-made?}))

;; Rebound simulation function
(defn simulate-rebound
  "Simulates a rebound, selecting a player from all players on court"
  [off-team def-team]
  (let [off-players (vals off-team)
        def-players (vals def-team)
        all-players (concat off-players def-players)
        rebound-chances (map (fn [p] (if (some #{p} off-players) (:orb-perc p) (:drb-perc p))) all-players)
        total-rebound-chance (reduce + rebound-chances)]
    (loop [r (rand total-rebound-chance)
           [player & remaining] all-players
           [chance & chances] rebound-chances]
      (if (< r chance)
        player
        (recur (- r chance) remaining chances)))))

;; Assist simulation function
(defn simulate-assist
  "Simulates a potential assist on a made shot using the hybrid logic."
  [shooter shot-type team]
  (let [assist-chance (if (= shot-type :3p)
                        (:ast-perc-3p shooter)
                        (:ast-perc-2p shooter))]

    (when (< (rand) assist-chance)

      (let [shooter-pos (some (fn [[pos p]] (when (= p shooter) pos)) team)
            potential-assisters (vals (dissoc team shooter-pos))
            total-assist-weight (reduce + (map :ast-perc potential-assisters))]

        (when (pos? total-assist-weight)
          (loop [r (rand total-assist-weight)
                 [player & remaining] potential-assisters]
            (let [weight (:ast-perc player)]
              (if (< r weight)
                player 
                (recur (- r weight) remaining)))))))))

;; Initial game state - fix later, add quarters
(def initial-game-state
  {:game-clock 2880 ; 48 minutes * 60 seconds
   :shot-clock 24
   :offense :team-a 
   :defense :team-b
   :score {:team-a 0 :team-b 0}
   :teams {:team-a team-a :team-b team-b}})

;; Possesion time simulation - fix later, add more complex logic to the game
(defn simulate-possession-time
  "Returns a random amount of time a possession might take."
  [shot-clock]
  (+ 5 (rand-int (- shot-clock 6))))

(defn find-player-pos [team player]
  (some (fn [[pos p]] (when (= p player) pos)) team))

;; Run possesion function - simulate a possesion and update game state
(defn run-possession
  "Runs a single possession and returns the new game state."
  [game-state]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        def-team (get-in game-state [:teams def-team-id :on-court])
        finisher (select-finisher (vals off-team))
        finisher-pos (find-player-pos off-team finisher)
        shot-result (simulate-shot finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))
        is-3p? (= :3p (:shot-type shot-result))]


    (println "-------------------------")
    (println "Shot clock - " (:shot-clock game-state))
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println (:name finisher) "takes a" (name (:shot-type shot-result)) "pointer...")
    (println "Possesion time" possession-time)
    

    (if (:shot-made? shot-result)
      (let [assister (simulate-assist finisher (:shot-type shot-result) off-team)
            points (if (= :3p (:shot-type shot-result)) 3 2)
            assister-pos (when assister (find-player-pos off-team assister))]
        (if assister
          (println "It's good! Assist by" (:name assister))
          (println "It's good! Unassisted."))

        (-> game-state
            (update :game-clock - possession-time)
            (assoc :shot-clock 24)
            (update-in [:score off-team-id] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :pts] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fgm] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pm] (if is-3p? inc identity))
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
            (cond-> assister-pos (update-in [:teams off-team-id :on-court assister-pos :box-score :ast] inc))            (assoc :offense def-team-id)
            (assoc :defense off-team-id)))

      (let [rebounder (simulate-rebound off-team def-team)]
        (println "It's a miss!")
        (println "Rebound by:" (:name rebounder))

        (let [offensive-rebound? (some #(= rebounder %) (vals off-team))]
          (-> game-state
              (update :game-clock - possession-time)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
              (update-in [:teams (if offensive-rebound? off-team-id def-team-id) :on-court (find-player-pos (if offensive-rebound? off-team def-team) rebounder) :box-score :reb] inc)
              (assoc :shot-clock (if offensive-rebound?
                                   14
                                   24))
              (assoc :offense (if offensive-rebound?
                                off-team-id
                                def-team-id))
              (assoc :defense (if offensive-rebound?
                                def-team-id
                                off-team-id))))))))


(defn print-box-score [game-state]
  (doseq [team-id [:team-a :team-b]]
    (let [team (get-in game-state [:teams team-id])]
      (println "\n---" (:name team) "---")
      (println (format "%-25s %-5s %-5s %-5s %-5s %-5s" "PLAYER" "PTS" "REB" "AST" "FG" "3P"))
      (doseq [player (vals (:on-court team))]
        (let [bs (:box-score player)]
          (println (format "%-25s %-5s %-5s %-5s %s-%-5s %s-%s"
                           (:name player)
                           (:pts bs)
                           (:reb bs)
                           (:ast bs)
                           (:fgm bs)
                           (:fga bs)
                           (:3pm bs)
                           (:3pa bs))))))))


(defn -main
  "Simulates a series of possessions."
  [& args]
  (let [final-state (-> initial-game-state
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession
                        run-possession)]
    (println "\n--- FINAL STATE ---")
    (println "Score ->" (:name (:team-a (:teams initial-game-state)))  (get-in final-state [:score :team-a]) "-"
     (:name (:team-b (:teams initial-game-state))) (get-in final-state [:score :team-b]) )
    (println "Time Remaining:" (int (/ (:game-clock final-state) 60)) "minutes")
    (println "Possession:" (:name (get-in final-state [:teams (:offense final-state)])))
    (print-box-score final-state)))