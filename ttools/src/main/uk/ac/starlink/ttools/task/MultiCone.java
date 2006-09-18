package uk.ac.starlink.ttools.task;

/**
 * Performs a cone search query for each row of an input table, 
 * and concatenates the result as one big output table.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class MultiCone extends SingleMapperTask {
    public MultiCone() {
        super( new MultiConeMapper(),
               "Makes multiple cone search queries to the same service",
               new ChoiceMode(), true, true );
    }
}
