(() => {
  const existing = scripts.Scripts.pokemon || {};
  const originalPokemon = require("./sim/pokemon").Pokemon.prototype;

  const currentFaint = existing.faint || originalPokemon.faint;
  const currentClearVolatile = existing.clearVolatile || originalPokemon.clearVolatile;

  const faint = currentFaint.__championsPersistentFaint
    ? currentFaint
    : function (source = null, effect = null) {
        if (this.fainted || this.faintQueued || !this.baseSpecies?.forme?.startsWith("Mega")) {
          return currentFaint.apply(this, arguments);
        }

        const originalFormeChange = this.formeChange;
        const hadOwnFormeChange = Object.prototype.hasOwnProperty.call(this, "formeChange");
        const hadOwnCanMega = Object.prototype.hasOwnProperty.call(this, "canMegaEvo");
        const originalCanMega = this.canMegaEvo;
        const originalBaseSpecies = this.baseSpecies;

        this.formeChange = function (speciesId, from, permanent) {
          if (this.faintQueued && from == null && permanent && speciesId === originalBaseSpecies.baseSpecies) {
            return true;
          }
          return originalFormeChange.apply(this, arguments);
        };

        try {
          return currentFaint.apply(this, arguments);
        } finally {
          if (hadOwnFormeChange) {
            this.formeChange = originalFormeChange;
          } else {
            delete this.formeChange;
          }

          if (this.faintQueued) {
            this.baseSpecies = originalBaseSpecies;

            if (hadOwnCanMega) {
              this.canMegaEvo = originalCanMega;
            } else {
              delete this.canMegaEvo;
            }
          }
        }
      };

  const clearVolatile = currentClearVolatile.__championsPersistentClearVolatile
    ? currentClearVolatile
    : function () {
        if (!this.faintQueued && !this.fainted) {
          return currentClearVolatile.apply(this, arguments);
        }

        const originalSetSpecies = this.setSpecies;
        const hadOwnSetSpecies = Object.prototype.hasOwnProperty.call(this, "setSpecies");

        this.setSpecies = function (rawSpecies) {
          if (rawSpecies === this.baseSpecies) return this.species;
          return originalSetSpecies.apply(this, arguments);
        };

        try {
          return currentClearVolatile.apply(this, arguments);
        } finally {
          if (hadOwnSetSpecies) {
            this.setSpecies = originalSetSpecies;
          } else {
            delete this.setSpecies;
          }
        }
      };

  faint.__championsPersistentFaint = true;
  clearVolatile.__championsPersistentClearVolatile = true;

  return { faint, clearVolatile };
})()