/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.polyworld.viewer.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.geom.BaseVector2f;
import org.terasology.math.geom.Vector2f;
import org.terasology.polyworld.graph.Corner;
import org.terasology.polyworld.graph.Graph;
import org.terasology.polyworld.moisture.MoistureModel;
import org.terasology.polyworld.moisture.MoistureModelFacet;
import org.terasology.world.generation.Region;
import org.terasology.world.viewer.layers.AbstractFacetLayer;
import org.terasology.world.viewer.layers.Renders;
import org.terasology.world.viewer.layers.ZOrder;
import org.terasology.world.viewer.picker.CirclePickerClosest;

import com.google.common.base.Stopwatch;

/**
 * TODO Convert this into a more general class that supports different graph-based value look-ups
 * @author Martin Steiger
 */
@Renders(value = MoistureModelFacet.class, order = ZOrder.BIOME + 10)
public class MoistureModelFacetLayer extends AbstractFacetLayer {

    private static final Logger logger = LoggerFactory.getLogger(MoistureModelFacetLayer.class);

    /**
     * The radius multiplier for the visible circles
     */
    private float scale = 4f;

    public MoistureModelFacetLayer() {
     // use default settings
    }

    @Override
    public void render(BufferedImage img, Region region) {
        MoistureModelFacet facet = region.getFacet(MoistureModelFacet.class);

        Stopwatch sw = Stopwatch.createStarted();

        Graphics2D g = img.createGraphics();
        int dx = region.getRegion().minX();
        int dy = region.getRegion().minZ();
        g.translate(-dx, -dy);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Graph graph : facet.getKeys()) {
            MoistureModel model = facet.get(graph);
            draw(g, model, graph);
        }

        g.dispose();

        if (logger.isTraceEnabled()) {
            logger.debug("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void draw(Graphics2D g, MoistureModel model, Graph graph) {
        g.setColor(new Color(0x4040FF));
        for (Corner c : graph.getCorners()) {
            float moisture = model.getMoisture(c);
            float r = scale * moisture;
            BaseVector2f loc = c.getLocation();
            g.fill(new Ellipse2D.Float(loc.getX() - r, loc.getY() - r, 2 * r, 2 * r));
        }
    }

    @Override
    public String getWorldText(Region region, int wx, int wy) {

        MoistureModelFacet moistureModelFacet = region.getFacet(MoistureModelFacet.class);
        Graph graph = findGraph(moistureModelFacet.getKeys(), wx, wy);

        if (graph != null) {
            MoistureModel model = moistureModelFacet.get(graph);

            Vector2f cursor = new Vector2f(wx, wy);

            // Use the value as radius, but clamp it to some minimum value so it
            // remains large enough to be hovered with the mouse cursor
            Function<Corner, Float> radiusFunc = c -> Math.max(2f, model.getMoisture(c) * scale);
            CirclePickerClosest<Corner> picker = new CirclePickerClosest<>(cursor, radiusFunc);

            for (Corner c : graph.getCorners()) {
                picker.offer(c.getLocation(), c);
            }

            if (picker.getClosest() != null) {
                float moisture = model.getMoisture(picker.getClosest());
                return String.format("Moisture: %.2f", moisture);
            }
        }

        return null;
    }

    private Graph findGraph(Collection<Graph> keys, int wx, int wy) {
        for (Graph graph : keys) {
            if (graph.getBounds().contains(wx, wy)) {
                return graph;
            }
        }

        return null;
    }
}
