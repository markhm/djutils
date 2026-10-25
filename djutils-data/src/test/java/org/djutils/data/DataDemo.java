package org.djutils.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.djunits.quantity.Duration;
import org.djunits.quantity.Temperature;
import org.djunits.quantity.Time;
import org.djutils.data.csv.CsvData;
import org.djutils.data.csv.TsvData;
import org.djutils.data.json.JsonData;
import org.djutils.data.serialization.TextSerializationException;
import org.djutils.data.xml.XmlData;

/**
 * DataDemo.java demonstration code used in the documentation.
 * <p>
 * Copyright (c) 2020-2026 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://djutils.org/docs/current/djutils/licenses.html">DJUTILS License</a>.
 * <p>
 * @author Alexander Verbraeck
 * @author Peter Knoppers
 * @author Wouter Schakel
 */
public final class DataDemo
{
    /**
     * Do not instantiate.
     */
    private DataDemo()
    {
        // Do not instantiate
    }

    /**
     * Demonstration code.
     * @param args the command line arguments (not used)
     * @throws TextSerializationException ...
     * @throws IOException ...
     * @throws XMLStreamException ...
     */
    public static void main(final String[] args) throws IOException, TextSerializationException, XMLStreamException
    {
        System.out.println("Example using java basic types");
        example1();
        System.out.println("Example using DJUNITS");
        example2();
    }

    /**
     * Straight forward example.
     * @throws TextSerializationException ...
     * @throws IOException ...
     * @throws XMLStreamException ...
     */
    public static void example1() throws IOException, TextSerializationException, XMLStreamException
    {
        Column<Integer> timeStamp = new Column<>("timeStamp", "time rounded to nearest second", int.class, "");
        Column<Double> temperature = new Column<>("temperature", "engine temperature in Celcius", double.class, "");
        Column<String> remark = new Column<>("remark", "remark", String.class, "");
        List<Column<?>> columns = List.of(timeStamp, temperature, remark);
        ListTable table = new ListTable("engineTemperatureData", "engine temperature samples", columns);

        System.out.println(table);

        Object[] rowData = new Object[] {600, 18.0, "starting engine"};
        table.addRow(rowData);

        Map<String, Object> map = new HashMap<>();
        map.put("remark", "leaving parking lot");
        map.put("temperature", 28.5);
        map.put("timeStamp", 660);
        table.addRowByColumnIds(map);

        for (Row row : table)
        {
            for (int column = 0; column < table.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + row.getValues()[column]);
            }
        }
        for (Row row : table)
        {
            System.out.println("timeStamp=" + row.getValue("timeStamp") + ", temperature=" + row.getValue("temperature") + ", "
                    + row.getValue("remark"));
        }

        System.out.println("JSON");
        JsonData.writeData("C:/Temp/example1.json", table);

        Table readBack = JsonData.readData("C:/Temp/example1.json");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }

        System.out.println("XML");
        XmlData.writeData("C:/Temp/example1.xml", table);

        readBack = XmlData.readData("C:/Temp/example1.xml");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }

        System.out.println("CSV");
        CsvData.writeData("C:/Temp/example1.csv", "C:/Temp/example1.csvm", table);

        readBack = CsvData.readData("C:/Temp/example1.csv", "C:/Temp/example1.csvm");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }

        System.out.println("TSV");
        TsvData.writeData("C:/Temp/example1.tsv", "C:/Temp/example1.tsvm", table);

        readBack = TsvData.readData("C:/Temp/example1.tsv", "C:/Temp/example1.tsvm");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }

        table.addRow(new Object[] {720, Double.NaN, "can we store a NaN value?"});
        table.addRow(new Object[] {780, Double.POSITIVE_INFINITY, "can we store positive infinity?"});
        table.addRow(new Object[] {840, Double.NEGATIVE_INFINITY, "can we store negative infinity?"});
        System.out.println("JSON");
        JsonData.writeData("C:/Temp/exampleNaN.json", table);

        readBack = JsonData.readData("C:/Temp/exampleNaN.json");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }
        CsvData.writeData("C:/Temp/exampleNaN.csv", "C:/Temp/exampleNaN.csvm", table);

    }

    /**
     * Example using DJUNITS strongly types values.
     * @throws TextSerializationException ...
     * @throws IOException ...
     */
    public static void example2() throws IOException, TextSerializationException
    {
        Column<Time> timeStamp = new Column<>("timeStamp", "time rounded to nearest second", Time.class, "s");
        Column<Temperature> temperature = new Column<>("temperature", "engine temperature in Celcius", Temperature.class, "K");
        Column<String> remark = new Column<>("remark", "remark", String.class, "");
        List<Column<?>> columns = new ArrayList<>();
        columns.add(timeStamp);
        columns.add(temperature);
        columns.add(remark);
        ListTable table = new ListTable("engineTemperatureData", "engine temperature samples", columns);

        System.out.println(table);

        Object[] record = new Object[] {new Time(600, Duration.Unit.s, Time.Reference.UNIX, false),
                new Temperature(18.0, Temperature.Unit.degC), "starting engine"};
        table.addRow(record);

        Map<String, Object> map = new HashMap<>();
        map.put("remark", "leaving parking lot");
        map.put("temperature", new Temperature(28.5, Temperature.Unit.degC));
        map.put("timeStamp", new Time(660, Duration.Unit.s, Time.Reference.UNIX, false));
        table.addRowByColumnIds(map);

        for (Row dataRecord : table)
        {
            for (int column = 0; column < table.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }
        for (Row dataRecord : table)
        {
            System.out.println("timeStamp=" + dataRecord.getValue("timeStamp") + ", temperature="
                    + dataRecord.getValue("temperature") + ", " + dataRecord.getValue("remark"));
        }

        System.out.println("JSON");
        JsonData.writeData("C:/Temp/example2.json", table);

        Table readBack = JsonData.readData("C:/Temp/example2.json");
        for (Row dataRecord : readBack)
        {
            for (int column = 0; column < readBack.getNumberOfColumns(); column++)
            {
                System.out.println("column " + column + ": " + dataRecord.getValues()[column]);
            }
        }

    }

}
