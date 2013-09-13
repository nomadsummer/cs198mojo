/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.code.peersim.starstream.protocol;

import com.google.code.peersim.starstream.controls.ChunkUtils.Chunk;

/**
 * This interface must be implemented by all those components interested in
 * receiving notifications related to *-Stream protocol events.
 *
 * @author frusso
 * @version 0.1
 * @since 0.1
 */
public interface StarStreamProtocolListenerIfc {

  /**
   * This method is invoked whenever a new chunk is saved in the *-Stream local
   * store, either if the chunk arrived via *-Stream or via Pastry.
   *
   * @param chunk The new chunk that has been just stored
   */
  public void notifyNewChunk(Chunk<?> chunk);
}