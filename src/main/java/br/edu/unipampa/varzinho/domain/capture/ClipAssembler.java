package br.edu.unipampa.varzinho.domain.capture;

import java.nio.file.Path;
import java.util.List;

/**
 * Turns the seconds a camera kept into a playable file.
 *
 * <p>The only thing the domain knows about real video: how the file is actually made
 * lives outside the domain, behind this interface, the same way persistence lives
 * behind a repository.
 */
public interface ClipAssembler {

    /**
     * Writes the window as one video file at the target.
     *
     * @param window the seconds to keep, oldest first
     * @param target where the file must end up
     * @throws br.edu.unipampa.varzinho.exception.ClipAssemblyException if the file could
     *         not be written
     */
    void assemble(List<Frame> window, Path target);
}
