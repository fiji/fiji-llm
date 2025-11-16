/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.llm.macro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of ImageJ macro built-in functions with detailed documentation. This
 * class provides a searchable database of all ImageJ macro functions from
 * https://imagej.net/ij/developer/macro/functions.html
 */
public class MacroFunctionRegistry {

	private static final List<MacroFunction> FUNCTIONS = new ArrayList<>();
	private static final Set<String> CATEGORIES = new HashSet<>();

	static {
		// Initialize the function registry with all documented macro functions
		initializeFunctions();
	}

	private static void initializeFunctions() {
		// Math Functions
		addFunction("abs(n)", "Math", "Returns the absolute value of n.");
		addFunction("acos(n)", "Math",
			"Returns the inverse cosine (in radians) of n.");
		addFunction("asin(n)", "Math",
			"Returns the inverse sine (in radians) of n.");
		addFunction("atan(n)", "Math",
			"Calculates the inverse tangent (arctangent) of n. Returns a value in the range -PI/2 through PI/2.");
		addFunction("atan2(y, x)", "Math",
			"Calculates the inverse tangent of y/x and returns an angle in the range -PI to PI, using the signs of the arguments to determine the quadrant.");
		addFunction("cos(angle)", "Math",
			"Returns the cosine of an angle (in radians).");
		addFunction("sin(angle)", "Math",
			"Returns the sine of an angle (in radians).");
		addFunction("tan(angle)", "Math",
			"Returns the tangent of an angle (in radians).");
		addFunction("exp(n)", "Math",
			"Returns the exponential number e (i.e., 2.718...) raised to the power of n.");
		addFunction("floor(n)", "Math",
			"Returns the largest value that is not greater than n and is equal to an integer.");
		addFunction("log(n)", "Math",
			"Returns the natural logarithm (base e) of n.");
		addFunction("pow(base, exponent)", "Math",
			"Returns the value of base raised to the power of exponent.");
		addFunction("sqrt(n)", "Math",
			"Returns the square root of n. Returns NaN if n is less than zero.");
		addFunction("round(n)", "Math", "Returns the closest integer to n.");
		addFunction("maxOf(n1, n2)", "Math", "Returns the greater of two values.");
		addFunction("minOf(n1, n2)", "Math", "Returns the smaller of two values.");
		addFunction("random()", "Math",
			"Returns a uniformly distributed pseudorandom number between 0 and 1.");

		// Array Functions
		addFunction("Array.concat(array1, array2, ...)", "Array",
			"Returns a new array created by joining two or more arrays or values.");
		addFunction("Array.copy(array)", "Array", "Returns a copy of array.");
		addFunction("Array.deleteValue(array, value)", "Array",
			"Returns a version of array where all numeric or string elements that contain value have been deleted.");
		addFunction("Array.deleteIndex(array, index)", "Array",
			"Returns a version of array where the element with the specified index has been deleted.");
		addFunction("Array.fill(array, value)", "Array",
			"Assigns the specified numeric value to each element of array.");
		addFunction("Array.filter(array, filter)", "Array",
			"Returns an array containing the elements of 'array' that contain 'filter'.");
		addFunction("Array.findMaxima(array, tolerance)", "Array",
			"Returns an array holding the peak positions.");
		addFunction("Array.findMinima(array, tolerance)", "Array",
			"Returns an array holding the minima positions.");
		addFunction("Array.fourier(array, windowType)", "Array",
			"Calculates and returns the Fourier amplitudes of array.");
		addFunction("Array.getSequence(n)", "Array",
			"Returns an array containing the numeric sequence 0,1,2...n-1.");
		addFunction("Array.getStatistics(array, min, max, mean, stdDev)", "Array",
			"Returns the min, max, mean, and stdDev of array.");
		addFunction("Array.print(array)", "Array",
			"Prints the array on a single line.");
		addFunction("Array.rankPositions(array)", "Array",
			"Returns the rank position indexes of array.");
		addFunction("Array.resample(array, len)", "Array",
			"Returns an array which is linearly resampled to a different length.");
		addFunction("Array.reverse(array)", "Array",
			"Reverses (inverts) the order of the elements in array.");
		addFunction("Array.show(array)", "Array",
			"Displays the contents of array in a window.");
		addFunction("Array.show(title, array1, array2, ...)", "Array",
			"Displays one or more arrays in a window with the specified title.");
		addFunction("Array.slice(array, start, end)", "Array",
			"Extracts a part of an array and returns it.");
		addFunction("Array.sort(array)", "Array",
			"Sorts array, which must contain all numbers or all strings.");
		addFunction("Array.sort(array1, array2, array3, ...)", "Array",
			"Sorts multiple arrays, where all arrays adopt the sort order of array1.");
		addFunction("Array.trim(array, n)", "Array",
			"Returns an array that contains the first n elements of array.");
		addFunction("Array.rotate(array, d)", "Array",
			"Rotates the array elements by 'd' steps.");
		addFunction("Array.getVertexAngles(xArr, yArr, arm)", "Array",
			"From a closed contour, returns an array holding vertex angles in degrees.");

		// String Functions
		addFunction("charCodeAt(string, index)", "String",
			"Returns the Unicode value of the character at the specified index in string.");
		addFunction("fromCharCode(value1, ..., valueN)", "String",
			"Takes one or more Unicode values and returns a string.");
		addFunction("indexOf(string, substring)", "String",
			"Returns the index within string of the first occurrence of substring.");
		addFunction("indexOf(string, substring, fromIndex)", "String",
			"Returns the index within string of the first occurrence of substring, starting at fromIndex.");
		addFunction("lastIndexOf(string, substring)", "String",
			"Returns the index within string of the rightmost occurrence of substring.");
		addFunction("lengthOf(str)", "String",
			"Returns the length of a string or array.");
		addFunction("replace(string, old, new)", "String",
			"Returns a string that results from replacing all occurrences of old in string with new.");
		addFunction("startsWith(string, prefix)", "String",
			"Returns true if string starts with prefix.");
		addFunction("endsWith(string, suffix)", "String",
			"Returns true if string ends with suffix.");
		addFunction("substring(string, index1, index2)", "String",
			"Returns a substring of string from index1 to index2-1.");
		addFunction("substring(string, index)", "String",
			"Returns a substring of string starting at index to the end.");
		addFunction("toLowerCase(string)", "String",
			"Returns a new string with all characters converted to lower case.");
		addFunction("toUpperCase(string)", "String",
			"Returns a new string with all characters converted to upper case.");
		addFunction("matches(string, regex)", "String",
			"Returns true if string matches the specified regular expression.");
		addFunction("split(string, delimiters)", "String",
			"Breaks a string into an array of substrings.");
		addFunction("d2s(n, decimalPlaces)", "String",
			"Converts the number n into a string using the specified number of decimal places.");
		addFunction("toString(number)", "String",
			"Returns a decimal string representation of number.");
		addFunction("toString(number, decimalPlaces)", "String",
			"Converts number into a string, using the specified number of decimal places.");
		addFunction("parseFloat(string)", "String",
			"Converts the string argument to a number and returns it.");
		addFunction("parseInt(string)", "String",
			"Converts string to an integer and returns it.");
		addFunction("parseInt(string, radix)", "String",
			"Converts string to an integer using the specified radix (base).");
		addFunction("String.resetBuffer()", "String",
			"Resets (clears) the buffer.");
		addFunction("String.append(str)", "String", "Appends str to the buffer.");
		addFunction("String.buffer()", "String",
			"Returns the contents of the buffer.");
		addFunction("String.copy(str)", "String", "Copies str to the clipboard.");
		addFunction("String.paste()", "String",
			"Returns the contents of the clipboard.");
		addFunction("String.format(format, n1, n2, ...)", "String",
			"Returns a formatted string using the specified format and numbers.");
		addFunction("String.pad(n, length)", "String",
			"Pads 'n' with leading zeros so that it is 'length' characters wide.");
		addFunction("String.join(array)", "String",
			"Creates a comma-delimited string from the elements of 'array'.");
		addFunction("String.join(array, delimiter)", "String",
			"Creates a string from the elements of 'array' with the specified delimiter.");
		addFunction("String.trim(string)", "String",
			"Returns a copy of 'string' that has leading and trailing whitespace omitted.");

		// Image Functions
		addFunction("Image.title", "Image", "The title of the active image.");
		addFunction("Image.width", "Image", "The width of the active image.");
		addFunction("Image.height", "Image", "The height of the active image.");
		addFunction("Image.copy()", "Image",
			"Copies the contents of the current selection, or the entire image if there is no selection, to the internal clipboard.");
		addFunction("Image.paste(x, y)", "Image",
			"Inserts the contents of the internal clipboard at the specified location in the active image.");
		addFunction("Image.paste(x, y, mode)", "Image",
			"Inserts the contents of the internal clipboard at x,y using the specified transfer mode.");
		addFunction("getImageID()", "Image",
			"Returns the unique ID (a negative number) of the active image.");
		addFunction("getTitle()", "Image",
			"Returns the title of the current image.");
		addFunction("getWidth()", "Image",
			"Returns the width in pixels of the current image.");
		addFunction("getHeight()", "Image",
			"Returns the height in pixels of the current image.");
		addFunction("getZoom()", "Image",
			"Returns the magnification of the active image.");
		addFunction("getImageInfo()", "Image",
			"Returns a string containing the text that would be displayed by the Image>Show Info command.");
		addFunction("getInfo(string)", "Image",
			"Returns information about the image or ImageJ system.");
		addFunction("bitDepth()", "Image",
			"Returns the bit depth of the active image: 8, 16, 24 (RGB) or 32 (float).");
		addFunction("rename(name)", "Image",
			"Changes the title of the active image to the string name.");
		addFunction("close()", "Image", "Closes the active image.");
		addFunction("close(pattern)", "Image",
			"Closes windows whose title matches 'pattern'.");
		addFunction("close(\"*\")", "Image", "Closes all image windows.");
		addFunction("close(\"\\\\Others\")", "Image",
			"Closes all images except for the front image.");
		addFunction("open(path)", "Image",
			"Opens and displays a tiff, dicom, fits, pgm, jpeg, bmp, gif, lut, roi, or text file.");
		addFunction("open(path, n)", "Image",
			"Opens the nth image in the TIFF stack specified by path.");
		addFunction("save(path)", "Image",
			"Saves an image, lookup table, selection or text window to the specified file path.");
		addFunction("saveAs(format, path)", "Image",
			"Saves the active image to the specified file path in the specified format.");
		addFunction("saveAs(format)", "Image",
			"Saves the active image, prompting for a file path.");
		addFunction("newImage(title, type, width, height, depth)", "Image",
			"Opens a new image or stack using the name title.");

		// Selection/ROI Functions
		addFunction("makeRectangle(x, y, width, height)", "Selection",
			"Creates a rectangular selection.");
		addFunction("makeRectangle(x, y, width, height, arcSize)", "Selection",
			"Creates a rounded rectangular selection.");
		addFunction("makeOval(x, y, width, height)", "Selection",
			"Creates an elliptical selection.");
		addFunction("makeLine(x1, y1, x2, y2)", "Selection",
			"Creates a new straight line selection.");
		addFunction("makeLine(x1, y1, x2, y2, lineWidth)", "Selection",
			"Creates a straight line selection with the specified width.");
		addFunction("makeLine(x1, y1, x2, y2, x3, y3, ...)", "Selection",
			"Creates a segmented line selection.");
		addFunction("makePolygon(x1, y1, x2, y2, x3, y3, ...)", "Selection",
			"Creates a polygonal selection.");
		addFunction("makeSelection(type, xpoints, ypoints)", "Selection",
			"Creates a selection from a list of XY coordinates.");
		addFunction("makeArrow(x1, y1, x2, y2, style)", "Selection",
			"Creates an arrow selection.");
		addFunction("makePoint(x, y)", "Selection",
			"Creates a point selection at the specified location.");
		addFunction("makePoint(x, y, options)", "Selection",
			"Creates a point selection with specified options.");
		addFunction("makeText(string, x, y)", "Selection",
			"Creates a text selection.");
		addFunction("makeEllipse(x1, y1, x2, y2, aspectRatio)", "Selection",
			"Creates an elliptical selection.");
		addFunction("makeRotatedRectangle(x1, y1, x2, y2, width)", "Selection",
			"Creates a rotated rectangular selection.");
		addFunction("getSelectionBounds(x, y, width, height)", "Selection",
			"Returns the smallest rectangle that can completely contain the current selection.");
		addFunction("getSelectionCoordinates(xpoints, ypoints)", "Selection",
			"Returns two arrays containing the X and Y coordinates of the current selection.");
		addFunction("selectionType()", "Selection",
			"Returns the selection type (0=rectangle, 1=oval, 2=polygon, etc.).");
		addFunction("selectionName()", "Selection",
			"Returns the name of the current selection.");
		addFunction("setSelectionLocation(x, y)", "Selection",
			"Moves the current selection to (x, y).");
		addFunction("setSelectionName(name)", "Selection",
			"Sets the name of the current selection.");
		addFunction("Roi.contains(x, y)", "Selection",
			"Returns true if the point x,y is inside the current selection.");
		addFunction("Roi.getBounds(x, y, width, height)", "Selection",
			"Returns the location and size of the selection's bounding rectangle.");
		addFunction("Roi.getFloatBounds(x, y, width, height)", "Selection",
			"Returns the location and size as real numbers.");
		addFunction("Roi.getCoordinates(xpoints, ypoints)", "Selection",
			"Returns the x and y coordinates that define this selection.");
		addFunction("Roi.setStrokeColor(color)", "Selection",
			"Sets the selection stroke color.");
		addFunction("Roi.setStrokeColor(red, green, blue)", "Selection",
			"Sets the selection stroke color with RGB values.");
		addFunction("Roi.setStrokeColor(rgb)", "Selection",
			"Sets the selection stroke color with an RGB integer.");
		addFunction("Roi.setStrokeWidth(width)", "Selection",
			"Sets the selection stroke width.");
		addFunction("Roi.setFillColor(color)", "Selection",
			"Sets the selection fill color.");
		addFunction("Roi.setFillColor(red, green, blue)", "Selection",
			"Sets the selection fill color with RGB values.");
		addFunction("Roi.setFillColor(rgb)", "Selection",
			"Sets the selection fill color with an RGB integer.");
		addFunction("Roi.setName(name)", "Selection", "Sets the selection name.");
		addFunction("Roi.getName()", "Selection", "Returns the selection name.");
		addFunction("Roi.size()", "Selection",
			"Returns the size of the current selection in points.");

		// ROI Manager Functions
		addFunction("roiManager(command)", "ROIManager",
			"Runs ROI Manager commands (add, delete, draw, measure, etc.).");
		addFunction("roiManager(command, args)", "ROIManager",
			"Runs ROI Manager commands with arguments.");
		addFunction("RoiManager.getName(index)", "ROIManager",
			"Returns the name of the selection with the specified index.");
		addFunction("RoiManager.select(index)", "ROIManager",
			"Activates the selection at the specified index.");
		addFunction("RoiManager.selectByName(name)", "ROIManager",
			"Activates the selection with the specified name.");
		addFunction("RoiManager.size()", "ROIManager",
			"Returns the number of ROIs in the ROI Manager list.");

		// File Functions
		addFunction("File.append(string, path)", "File",
			"Appends string to the end of the specified file.");
		addFunction("File.close(f)", "File", "Closes the specified file.");
		addFunction("File.copy(path1, path2)", "File", "Copies a file.");
		addFunction("File.delete(path)", "File",
			"Deletes the specified file or directory.");
		addFunction("File.exists(path)", "File",
			"Returns true if the specified file exists.");
		addFunction("File.getName(path)", "File",
			"Returns the file name from path.");
		addFunction("File.getNameWithoutExtension(path)", "File",
			"Returns the file name without extension.");
		addFunction("File.getDirectory(path)", "File",
			"Returns the directory from path.");
		addFunction("File.getParent(path)", "File",
			"Returns the parent of the file specified by path.");
		addFunction("File.isDirectory(path)", "File",
			"Returns true if the specified file is a directory.");
		addFunction("File.isFile(path)", "File",
			"Returns true if the specified file is not a directory.");
		addFunction("File.length(path)", "File",
			"Returns the length in bytes of the specified file.");
		addFunction("File.makeDirectory(path)", "File", "Creates a directory.");
		addFunction("File.open(path)", "File",
			"Creates a new text file and returns a file variable that refers to it.");
		addFunction("File.openAsString(path)", "File",
			"Opens a text file and returns the contents as a string.");
		addFunction("File.openAsRawString(path)", "File",
			"Opens a file and returns up to the first 5,000 bytes as a string.");
		addFunction("File.openAsRawString(path, count)", "File",
			"Opens a file and returns up to the first count bytes as a string.");
		addFunction("File.openDialog(title)", "File",
			"Displays a file open dialog and returns the path to the file chosen by the user.");
		addFunction("File.rename(path1, path2)", "File",
			"Renames, or moves, a file or directory.");
		addFunction("File.saveString(string, path)", "File",
			"Saves string as a file.");
		addFunction("getDirectory(string)", "File",
			"Displays a 'choose directory' dialog and returns the selected directory.");
		addFunction("getDir(string)", "File",
			"An alias of getDirectory since 1.49q.");
		addFunction("getFileList(directory)", "File",
			"Returns an array containing the names of the files in the specified directory.");

		// Dialog Functions
		addFunction("Dialog.create(title)", "Dialog",
			"Creates a modal dialog box with the specified title.");
		addFunction("Dialog.createNonBlocking(title)", "Dialog",
			"Creates a non-modal dialog box with the specified title.");
		addFunction("Dialog.addString(label, initialText)", "Dialog",
			"Adds a text field to the dialog.");
		addFunction("Dialog.addString(label, initialText, columns)", "Dialog",
			"Adds a text field to the dialog with specified width.");
		addFunction("Dialog.addNumber(label, default)", "Dialog",
			"Adds a numeric field to the dialog.");
		addFunction(
			"Dialog.addNumber(label, default, decimalPlaces, columns, units)",
			"Dialog", "Adds a numeric field with full options.");
		addFunction("Dialog.addSlider(label, min, max, default)", "Dialog",
			"Adds a slider controlled numeric field to the dialog.");
		addFunction("Dialog.addCheckbox(label, default)", "Dialog",
			"Adds a checkbox to the dialog.");
		addFunction("Dialog.addCheckboxGroup(rows, columns, labels, defaults)",
			"Dialog", "Adds a grid of checkboxes to the dialog.");
		addFunction(
			"Dialog.addRadioButtonGroup(label, items, rows, columns, default)",
			"Dialog", "Adds a group of radio buttons to the dialog.");
		addFunction("Dialog.addChoice(label, items)", "Dialog",
			"Adds a popup menu to the dialog.");
		addFunction("Dialog.addChoice(label, items, default)", "Dialog",
			"Adds a popup menu with a default selection.");
		addFunction("Dialog.addDirectory(label, defaultPath)", "Dialog",
			"Adds a directory field and 'Browse' button.");
		addFunction("Dialog.addFile(label, defaultPath)", "Dialog",
			"Adds a file field and 'Browse' button.");
		addFunction("Dialog.addMessage(string)", "Dialog",
			"Adds a message to the dialog.");
		addFunction("Dialog.addMessage(string, fontSize, fontColor)", "Dialog",
			"Adds a message with specified font size and color.");
		addFunction("Dialog.addImage(pathOrURL)", "Dialog",
			"Adds an image to the dialog.");
		addFunction("Dialog.addImageChoice(label)", "Dialog",
			"Adds a popup menu that lists the currently open images.");
		addFunction("Dialog.addHelp(url)", "Dialog",
			"Adds a 'Help' button that opens the specified URL.");
		addFunction("Dialog.addToSameRow()", "Dialog",
			"Makes the next item added appear on the same row as the previous item.");
		addFunction("Dialog.setInsets(top, left, bottom)", "Dialog",
			"Overrides the default insets (margins) for the next component.");
		addFunction("Dialog.setLocation(x, y)", "Dialog",
			"Sets the screen location where this dialog will be displayed.");
		addFunction("Dialog.show()", "Dialog",
			"Displays the dialog and waits until the user clicks 'OK' or 'Cancel'.");
		addFunction("Dialog.getString()", "Dialog",
			"Returns a string containing the contents of the next text field.");
		addFunction("Dialog.getNumber()", "Dialog",
			"Returns the contents of the next numeric field.");
		addFunction("Dialog.getCheckbox()", "Dialog",
			"Returns the state (true or false) of the next checkbox.");
		addFunction("Dialog.getChoice()", "Dialog",
			"Returns the selected item from the next popup menu.");
		addFunction("Dialog.getRadioButton()", "Dialog",
			"Returns the selected item from the next radio button group.");
		addFunction("Dialog.getImageChoice()", "Dialog",
			"Returns the title of the image selected in the next image choice popup menu.");
		addFunction("getBoolean(message)", "Dialog",
			"Displays a dialog box with Yes/No/Cancel buttons.");
		addFunction("getBoolean(message, yesLabel, noLabel)", "Dialog",
			"Displays a dialog box with custom button labels.");
		addFunction("getNumber(prompt, defaultValue)", "Dialog",
			"Displays a dialog box and returns the number entered by the user.");
		addFunction("getString(prompt, default)", "Dialog",
			"Displays a dialog box and returns the string entered by the user.");
		addFunction("showMessage(message)", "Dialog",
			"Displays a message in a dialog box.");
		addFunction("showMessage(title, message)", "Dialog",
			"Displays a message in a dialog box with a title.");
		addFunction("showMessageWithCancel(message)", "Dialog",
			"Displays a message in a dialog box with 'OK' and 'Cancel' buttons.");
		addFunction("showMessageWithCancel(title, message)", "Dialog",
			"Displays a message with title and 'OK' and 'Cancel' buttons.");

		// Drawing Functions
		addFunction("drawLine(x1, y1, x2, y2)", "Drawing",
			"Draws a line between (x1, y1) and (x2, y2).");
		addFunction("drawOval(x, y, width, height)", "Drawing",
			"Draws the outline of an oval.");
		addFunction("drawRect(x, y, width, height)", "Drawing",
			"Draws the outline of a rectangle.");
		addFunction("drawString(text, x, y)", "Drawing",
			"Draws text at the specified location.");
		addFunction("drawString(text, x, y, background)", "Drawing",
			"Draws text at the specified location with a filled background.");
		addFunction("fillOval(x, y, width, height)", "Drawing",
			"Fills an oval bounded by the specified rectangle with the current drawing color.");
		addFunction("fillRect(x, y, width, height)", "Drawing",
			"Fills the specified rectangle with the current drawing color.");
		addFunction("fill()", "Drawing",
			"Fills the image or selection with the current drawing color.");
		addFunction("floodFill(x, y)", "Drawing",
			"Fills, with the foreground color, pixels that are connected to and the same color as the pixel at (x, y).");
		addFunction("floodFill(x, y, mode)", "Drawing",
			"Fills with flood fill mode option (8-connected, etc.).");
		addFunction("lineTo(x, y)", "Drawing",
			"Draws a line from current location to (x, y).");
		addFunction("moveTo(x, y)", "Drawing",
			"Sets the current drawing location.");
		addFunction("setColor(r, g, b)", "Drawing",
			"Sets the drawing color with RGB values.");
		addFunction("setColor(value)", "Drawing",
			"Sets the drawing color with a numeric value.");
		addFunction("setColor(string)", "Drawing",
			"Sets the drawing color with a color name or hex value.");
		addFunction("setFont(name, size)", "Drawing",
			"Sets the font used by the drawString function.");
		addFunction("setFont(name, size, style)", "Drawing",
			"Sets the font with name, size, and style (bold/italic).");
		addFunction("setFont(\"user\")", "Drawing",
			"Sets the font to the one defined in Edit>Options>Fonts.");
		addFunction("setLineWidth(width)", "Drawing",
			"Specifies the line width used by drawLine, lineTo, drawRect and drawOval.");
		addFunction("setJustification(mode)", "Drawing",
			"Specifies the justification used by drawString().");
		addFunction("getStringWidth(string)", "Drawing",
			"Returns the width in pixels of the specified string.");
		addFunction("autoUpdate(boolean)", "Drawing",
			"Controls whether the display is refreshed automatically.");

		// Overlay Functions
		addFunction("Overlay.moveTo(x, y)", "Overlay",
			"Sets the current drawing location.");
		addFunction("Overlay.lineTo(x, y)", "Overlay",
			"Draws a line from the current location to (x, y).");
		addFunction("Overlay.drawLine(x1, y1, x2, y2)", "Overlay",
			"Draws a line between (x1, y1) and (x2, y2).");
		addFunction("Overlay.drawRect(x, y, width, height)", "Overlay",
			"Draws a rectangle.");
		addFunction("Overlay.drawEllipse(x, y, width, height)", "Overlay",
			"Draws an ellipse.");
		addFunction("Overlay.drawString(text, x, y)", "Overlay",
			"Draws text at the specified location and adds it to the overlay.");
		addFunction("Overlay.drawString(text, x, y, angle)", "Overlay",
			"Draws text at the specified location and angle.");
		addFunction("Overlay.add()", "Overlay",
			"Adds the drawing created by Overlay functions to the overlay.");
		addFunction("Overlay.show()", "Overlay", "Displays the current overlay.");
		addFunction("Overlay.hide()", "Overlay", "Hides the current overlay.");
		addFunction("Overlay.hidden()", "Overlay",
			"Returns true if the overlay is hidden.");
		addFunction("Overlay.remove()", "Overlay", "Removes the current overlay.");
		addFunction("Overlay.clear()", "Overlay",
			"Resets the overlay without updating the display.");
		addFunction("Overlay.size()", "Overlay",
			"Returns the size (selection count) of the current overlay.");
		addFunction("Overlay.addSelection()", "Overlay",
			"Adds the current selection to the overlay.");
		addFunction("Overlay.addSelection(strokeColor)", "Overlay",
			"Adds the current selection with specified stroke color.");
		addFunction("Overlay.addSelection(strokeColor, strokeWidth)", "Overlay",
			"Adds the current selection with color and width.");
		addFunction("Overlay.activateSelection(index)", "Overlay",
			"Activates the specified overlay selection.");
		addFunction("Overlay.removeSelection(index)", "Overlay",
			"Removes the specified selection from the overlay.");

		// Measurement/Analysis Functions
		addFunction("getStatistics(area, mean, min, max, std, histogram)",
			"Measurement",
			"Returns the area, average pixel value, minimum pixel value, maximum pixel value, standard deviation of the pixel values and histogram.");
		addFunction("getRawStatistics(nPixels, mean, min, max, std, histogram)",
			"Measurement",
			"Similar to getStatistics except the values returned are uncalibrated.");
		addFunction("getHistogram(values, counts, nBins)", "Measurement",
			"Returns the histogram of the current image or selection.");
		addFunction("getHistogram(values, counts, nBins, histMin, histMax)",
			"Measurement", "Returns the histogram with specified range.");
		addFunction("getPixel(x, y)", "Measurement",
			"Returns the raw value of the pixel at (x, y).");
		addFunction("getValue(x, y)", "Measurement",
			"Returns calibrated pixel values from 8 and 16 bit images and intensity values from RGB images.");
		addFunction("getValue(string)", "Measurement",
			"Returns a measurement result with the specified label.");
		addFunction("setPixel(x, y, value)", "Measurement",
			"Stores value at location (x, y) of the current image.");
		addFunction("getProfile()", "Measurement",
			"Runs Analyze>Plot Profile and returns the intensity values as an array.");
		addFunction("getResult(Column, row)", "Measurement",
			"Returns a measurement from the ImageJ results table.");
		addFunction("getResultString(Column, row)", "Measurement",
			"Returns a string from the ImageJ results table.");
		addFunction("getResultLabel(row)", "Measurement",
			"Returns the label of the specified row in the results table.");
		addFunction("setResult(Column, row, value)", "Measurement",
			"Adds an entry to the ImageJ results table or modifies an existing entry.");
		addFunction("updateResults()", "Measurement",
			"Updates the 'Results' window after the results table has been modified.");
		addFunction("nResults()", "Measurement",
			"Returns the current measurement counter value.");
		addFunction("calibrate(value)", "Measurement",
			"Uses the calibration function of the active image to convert a raw pixel value.");

		// Threshold/Adjustment Functions
		addFunction("setThreshold(lower, upper)", "Threshold",
			"Sets the lower and upper threshold levels.");
		addFunction("setThreshold(lower, upper, mode)", "Threshold",
			"Sets threshold with mode (red, black & white, etc.).");
		addFunction("setAutoThreshold()", "Threshold",
			"Uses the 'Default' method to determine the threshold.");
		addFunction("setAutoThreshold(method)", "Threshold",
			"Uses the specified method to determine the threshold.");
		addFunction("getThreshold(lower, upper)", "Threshold",
			"Returns the lower and upper threshold levels.");
		addFunction("resetThreshold()", "Threshold", "Disables thresholding.");
		addFunction("setMinAndMax(min, max)", "Threshold",
			"Sets the minimum and maximum displayed pixel values (display range).");
		addFunction("setMinAndMax(min, max, channels)", "Threshold",
			"Sets the display range of specified channels in an RGB image.");
		addFunction("getMinAndMax(min, max)", "Threshold",
			"Returns the minimum and maximum displayed pixel values (display range).");
		addFunction("resetMinAndMax()", "Threshold",
			"Resets the minimum and maximum displayed pixel values.");

		// Stack/Hyperstack Functions
		addFunction("Stack.getDimensions(width, height, channels, slices, frames)",
			"Stack", "Returns the dimensions of the current image.");
		addFunction("Stack.setDimensions(channels, slices, frames)", "Stack",
			"Sets the 3rd, 4th and 5th dimensions of the current stack.");
		addFunction("Stack.setChannel(n)", "Stack", "Displays channel n.");
		addFunction("Stack.setSlice(n)", "Stack", "Displays slice n.");
		addFunction("Stack.setFrame(n)", "Stack", "Displays frame n.");
		addFunction("Stack.getPosition(channel, slice, frame)", "Stack",
			"Returns the current position (channel, slice, frame).");
		addFunction("Stack.setPosition(channel, slice, frame)", "Stack",
			"Displays the specified channel, slice and frame.");
		addFunction("Stack.isHyperstack()", "Stack",
			"Returns true if the current image is a hyperstack.");
		addFunction("getSliceNumber()", "Stack",
			"Returns the number of the currently displayed stack image.");
		addFunction("setSlice(n)", "Stack",
			"Displays the nth slice of the active stack.");
		addFunction("nSlices()", "Stack",
			"Returns the number of images in the current stack.");

		// List Functions
		addFunction("List.set(key, value)", "List",
			"Adds a key/value pair to the list.");
		addFunction("List.get(key)", "List",
			"Returns the string value associated with key.");
		addFunction("List.getValue(key)", "List",
			"Returns the value associated with key as a number.");
		addFunction("List.size()", "List", "Returns the size of the list.");
		addFunction("List.clear()", "List", "Resets the list.");
		addFunction("List.setMeasurements()", "List",
			"Measures the current image or selection and loads the resulting keys and values into the list.");
		addFunction("List.setMeasurements(options)", "List",
			"Measures with options (e.g., 'limit').");

		// Utility Functions
		addFunction("beep()", "Utility", "Emits an audible beep.");
		addFunction("wait(n)", "Utility", "Delays (sleeps) for n milliseconds.");
		addFunction("print(string)", "Utility",
			"Outputs a string to the 'Log' window.");
		addFunction("print(string, arg1, arg2, ...)", "Utility",
			"Prints multiple arguments to the Log window.");
		addFunction("exit()", "Utility", "Terminates execution of the macro.");
		addFunction("exit(error message)", "Utility",
			"Terminates execution of the macro and displays an error message.");
		addFunction("eval(macro)", "Utility",
			"Evaluates (runs) one or more lines of macro code.");
		addFunction("eval(script, javascript)", "Utility",
			"Evaluates JavaScript code.");
		addFunction("eval(js, script)", "Utility", "Evaluates JavaScript code.");
		addFunction("eval(bsh, script)", "Utility", "Evaluates BeanShell code.");
		addFunction("eval(python, script)", "Utility", "Evaluates Python code.");
		addFunction("exec(string or strings)", "Utility",
			"Executes a native command and returns the output.");
		addFunction("run(command)", "Utility", "Executes an ImageJ menu command.");
		addFunction("run(command, options)", "Utility",
			"Executes an ImageJ menu command with arguments.");
		addFunction("doCommand(command)", "Utility",
			"Runs an ImageJ menu command in a separate thread.");
		addFunction("runMacro(name)", "Utility",
			"Runs the specified macro or script.");
		addFunction("runMacro(name, arg)", "Utility",
			"Runs the specified macro or script with a string argument.");
		addFunction("call(class.method, arg1, arg2, ...)", "Utility",
			"Calls a public static method in a Java class.");
		addFunction("call(class.method)", "Utility",
			"Calls a public static no-argument method in a Java class.");
		addFunction("getTime()", "Utility",
			"Returns the current time in milliseconds.");
		addFunction("getVersion()", "Utility",
			"Returns the ImageJ version number as a string.");
		addFunction("showStatus(message)", "Utility",
			"Displays a message in the ImageJ status bar.");
		addFunction("showProgress(progress)", "Utility",
			"Updates the ImageJ progress bar, where 0.0<=progress<=1.0.");
		addFunction("showProgress(currentIndex, finalIndex)", "Utility",
			"Updates the progress bar.");
		addFunction("getArgument()", "Utility",
			"Returns the string argument passed to the macro.");
		addFunction("requires(version)", "Utility",
			"Displays a message and aborts the macro if the ImageJ version is less than specified.");
		addFunction("debug(arg)", "Utility", "Calls the macro debugger.");
		addFunction("dump()", "Utility",
			"Writes the contents of the symbol table, the tokenized macro code and the variable stack to the Log window.");
		addFunction("snapshot()", "Utility",
			"Creates a backup copy of the current image that can be later restored using the reset function.");
		addFunction("reset()", "Utility",
			"Restores the backup image created by the snapshot function.");

		// Perspective/Transformation Functions
		addFunction("getVoxelSize(width, height, depth, unit)", "Transformation",
			"Returns the voxel size and unit of length.");
		addFunction("setVoxelSize(width, height, depth, unit)", "Transformation",
			"Defines the voxel dimensions and unit of length for the current image.");
		addFunction("getPixelSize(unit, pixelWidth, pixelHeight)", "Transformation",
			"Returns the unit of length and the pixel dimensions.");
		addFunction("setZCoordinate(z)", "Transformation",
			"Sets the Z coordinate used by getPixel(), setPixel() and changeValues().");
		addFunction("toScaled(x, y)", "Transformation",
			"Converts unscaled pixel coordinates to scaled coordinates.");
		addFunction("toScaled(x, y, z)", "Transformation",
			"Converts unscaled 3D coordinates to scaled coordinates.");
		addFunction("toScaled(length)", "Transformation",
			"Converts a horizontal length in pixels to a scaled length.");
		addFunction("toUnscaled(x, y)", "Transformation",
			"Converts scaled coordinates to unscaled pixel coordinates.");
		addFunction("toUnscaled(x, y, z)", "Transformation",
			"Converts scaled 3D coordinates to unscaled coordinates.");
		addFunction("toUnscaled(length)", "Transformation",
			"Converts a scaled horizontal length to a length in pixels.");

		// Window Functions
		addFunction("selectImage(id)", "Window",
			"Activates the image with the specified ID.");
		addFunction("selectImage(title)", "Window",
			"Activates the image with the specified title.");
		addFunction("selectWindow(name)", "Window",
			"Activates the window with the title specified.");
		addFunction("isOpen(id)", "Window",
			"Returns true if the image with the specified ID is open.");
		addFunction("isOpen(title)", "Window",
			"Returns true if the window with the specified title is open.");
		addFunction("isActive(id)", "Window",
			"Returns true if the image with the specified ID is active.");
		addFunction("nImages()", "Window", "Returns number of open images.");
		addFunction("setLocation(x, y)", "Window",
			"Moves the active window to a new location.");
		addFunction("setLocation(x, y, width, height)", "Window",
			"Moves and resizes the active image window.");
		addFunction("getLocationAndSize(x, y, width, height)", "Window",
			"Returns the location and size of the active image window.");
		addFunction("screenWidth()", "Window",
			"Returns the screen width in pixels.");
		addFunction("screenHeight()", "Window",
			"Returns the screen height in pixels.");

		// Color Functions
		addFunction("Color.set(string)", "Color", "Sets the drawing color.");
		addFunction("Color.set(value)", "Color",
			"Sets the drawing color with a numeric value.");
		addFunction("Color.setForeground(string)", "Color",
			"Sets the foreground color.");
		addFunction("Color.setForeground(r, g, b)", "Color",
			"Sets the foreground color with RGB values.");
		addFunction("Color.setForegroundValue(value)", "Color",
			"Sets the foreground color to grayscale.");
		addFunction("Color.setBackground(string)", "Color",
			"Sets the background color.");
		addFunction("Color.setBackground(r, g, b)", "Color",
			"Sets the background color with RGB values.");
		addFunction("Color.setBackgroundValue(value)", "Color",
			"Sets the background color to grayscale.");
		addFunction("Color.foreground()", "Color",
			"Returns the foreground color as a string.");
		addFunction("Color.background()", "Color",
			"Returns the background color as a string.");
		addFunction("Color.toString(r, g, b)", "Color",
			"Converts an r,g,b color to a string.");
		addFunction("Color.toArray(string)", "Color",
			"Converts a color to a three element array.");
		addFunction("Color.getLut(reds, greens, blues)", "Color",
			"Returns three arrays containing the red, green and blue intensity values from the current lookup table.");
		addFunction("Color.setLut(reds, greens, blues)", "Color",
			"Creates a new lookup table and assigns it to the current image.");
		addFunction("Color.wavelengthToColor(wavelength)", "Color",
			"Converts a wavelength (380-750 nm) into a color in string format.");
		addFunction("setForegroundColor(r, g, b)", "Color",
			"Sets the foreground color.");
		addFunction("setForegroundColor(rgb)", "Color",
			"Sets the foreground color with an RGB pixel value.");
		addFunction("setBackgroundColor(r, g, b)", "Color",
			"Sets the background color.");
		addFunction("setBackgroundColor(rgb)", "Color",
			"Sets the background color with an RGB pixel value.");

		// Plot Functions
		addFunction("Plot.create(title, xLabel, yLabel, xValues, yValues)", "Plot",
			"Generates a plot.");
		addFunction("Plot.create(title, categoryLabels, yLabel)", "Plot",
			"Generates a plot with category labels.");
		addFunction("Plot.add(type, xValues, yValues)", "Plot",
			"Adds a curve, set of points or error bars to a plot.");
		addFunction("Plot.add(type, xValues, yValues, label)", "Plot",
			"Adds data with a label to the plot.");
		addFunction("Plot.addHistogram(values, binWidth, binCenter)", "Plot",
			"Creates a staircase histogram.");
		addFunction("Plot.show()", "Plot", "Displays the plot.");
		addFunction("Plot.update()", "Plot",
			"Updates the plot in an existing plot window.");
		addFunction("Plot.setLimits(xMin, xMax, yMin, yMax)", "Plot",
			"Sets the range of the x-axis and y-axis of plots.");
		addFunction("Plot.getLimits(xMin, xMax, yMin, yMax)", "Plot",
			"Returns the current axis limits.");
		addFunction("Plot.setColor(color)", "Plot",
			"Specifies the color used in subsequent calls to Plot.add().");
		addFunction("Plot.setColor(color1, color2)", "Plot",
			"Sets color and fill color for the next data set.");
		addFunction("Plot.setLineWidth(width)", "Plot",
			"Specifies the width of the line used to draw a curve.");
		addFunction("Plot.addText(text, x, y)", "Plot",
			"Adds text to the plot at the specified location.");
		addFunction("Plot.setJustification(mode)", "Plot",
			"Specifies the justification used by Plot.addText().");
		addFunction("Plot.setLegend(labels, options)", "Plot",
			"Creates a legend for each of the data sets.");
		addFunction("Plot.getValues(xpoints, ypoints)", "Plot",
			"Returns the values displayed by clicking on 'List' in a plot.");

		// Fit Functions
		addFunction("Fit.doFit(equation, xpoints, ypoints)", "Fit",
			"Fits the specified equation to the points.");
		addFunction("Fit.doFit(equation, xpoints, ypoints, initialGuesses)", "Fit",
			"Fits equation with initial parameter guesses.");
		addFunction(
			"Fit.doWeightedFit(equation, xpoints, ypoints, weights, initialGuesses)",
			"Fit", "Fits equation to weighted points.");
		addFunction("Fit.rSquared()", "Fit", "Returns R^2.");
		addFunction("Fit.p(index)", "Fit",
			"Returns the value of the specified parameter.");
		addFunction("Fit.nParams()", "Fit", "Returns the number of parameters.");
		addFunction("Fit.f(x)", "Fit", "Returns the y value at x.");
		addFunction("Fit.nEquations()", "Fit", "Returns the number of equations.");
		addFunction("Fit.getEquation(index, name, formula)", "Fit",
			"Returns the name and formula of the specified equation.");
		addFunction("Fit.plot()", "Fit", "Plots the current curve fit.");

		// Batch Mode
		addFunction("setBatchMode(boolean)", "Batch",
			"Controls whether images are visible or hidden during macro execution.");
		addFunction("setBatchMode(exit and display)", "Batch",
			"Exits batch mode and displays all hidden images.");
		addFunction("setBatchMode(show)", "Batch",
			"Displays the active hidden image.");
		addFunction("setBatchMode(hide)", "Batch",
			"Enters batch mode and hides the active image.");

		// Other State Functions
		addFunction("is(property)", "State",
			"Returns various state information about ImageJ and the current image.");
		addFunction("isNaN(n)", "State",
			"Returns true if the value of the number n is NaN (Not-a-Number).");
		addFunction("isKeyDown(key)", "State",
			"Returns true if the specified key is pressed.");
		addFunction("setKeyDown(keys)", "State",
			"Simulates pressing the shift, alt or space keys.");
		addFunction("setOption(option, boolean)", "State",
			"Enables or disables ImageJ options.");
		addFunction("getDimensions(width, height, channels, slices, frames)",
			"State", "Returns the dimensions of the current image.");
		addFunction("getMetadata(property)", "State",
			"Returns the metadata from the current image.");
		addFunction("setMetadata(property, string)", "State",
			"Assigns metadata to the current image.");
		addFunction("getBoolean(message)", "State",
			"Gets boolean value from user.");
		addFunction("saveSettings()", "State",
			"Saves most Edit>Options submenu settings.");
		addFunction("restoreSettings()", "State",
			"Restores Edit>Options submenu settings.");

		// Property Functions
		addFunction("Property.get(key)", "Property",
			"Returns the image property associated with key.");
		addFunction("Property.getValue(key)", "Property",
			"Returns the image property as a number.");
		addFunction("Property.getNumber(key)", "Property",
			"Alias for Property.getValue(key).");
		addFunction("Property.set(key, property)", "Property",
			"Adds a key-value pair to the property list of the current image.");
		addFunction("Property.getInfo()", "Property",
			"Returns the image 'info' property string.");
		addFunction("Property.getSliceLabel()", "Property",
			"Returns the current slice label.");
		addFunction("Property.setSliceLabel(string)", "Property",
			"Sets the label of the current stack slice.");
		addFunction("Property.setSliceLabel(string, slice)", "Property",
			"Sets the label of the specified stack slice.");
		addFunction("Property.getList()", "Property",
			"Returns the properties as a string.");
		addFunction("Property.setList(string)", "Property",
			"Sets the properties from key/value pairs.");

		// Table Functions
		addFunction("Table.create(name)", "Table", "Creates or resets a table.");
		addFunction("Table.reset(name)", "Table", "Resets the specified table.");
		addFunction("Table.get(columnName, rowIndex)", "Table",
			"Returns the numeric value from the cell at the specified column and row.");
		addFunction("Table.getString(columnName, rowIndex)", "Table",
			"Returns a string value from the cell.");
		addFunction("Table.set(columnName, rowIndex, value)", "Table",
			"Assigns a numeric or string value to the cell.");
		addFunction("Table.getColumn(columnName)", "Table",
			"Returns the specified column as an array.");
		addFunction("Table.setColumn(columnName, array)", "Table",
			"Assigns an array to the specified column.");
		addFunction("Table.save(filePath)", "Table", "Saves a table.");
		addFunction("Table.open(filePath)", "Table", "Opens a table.");
		addFunction("Table.size()", "Table",
			"The number of rows in the current table.");
		addFunction("Table.update()", "Table",
			"Updates the window displaying the current table.");

		// Math Object Functions (ImageJ 1.52u+)
		addFunction("Math.abs(n)", "Math", "Returns the absolute value of n.");
		addFunction("Math.acos(n)", "Math",
			"Returns the inverse cosine (in radians) of n.");
		addFunction("Math.asin(n)", "Math",
			"Returns the inverse sine (in radians) of n.");
		addFunction("Math.atan(n)", "Math",
			"Returns the inverse tangent (arctangent) of n.");
		addFunction("Math.atan2(y, x)", "Math",
			"Calculates the inverse tangent of y/x.");
		addFunction("Math.ceil(n)", "Math",
			"Returns the smallest value that is >= n and is an integer.");
		addFunction("Math.cos(angle)", "Math",
			"Returns the cosine of an angle (in radians).");
		addFunction("Math.erf(x)", "Math",
			"Returns an approximation of the error function.");
		addFunction("Math.exp(n)", "Math", "Returns e raised to the power of n.");
		addFunction("Math.floor(n)", "Math",
			"Returns the largest value that is <= n and is an integer.");
		addFunction("Math.log(n)", "Math",
			"Returns the natural logarithm (base e) of n.");
		addFunction("Math.log10(n)", "Math", "Returns the base 10 logarithm of n.");
		addFunction("Math.min(n1, n2)", "Math",
			"Returns the smaller of two values.");
		addFunction("Math.max(n1, n2)", "Math",
			"Returns the larger of two values.");
		addFunction("Math.pow(base, exponent)", "Math",
			"Returns base raised to the power of exponent.");
		addFunction("Math.round(n)", "Math", "Returns the closest integer to n.");
		addFunction("Math.sin(angle)", "Math",
			"Returns the sine of an angle (in radians).");
		addFunction("Math.sqr(n)", "Math", "Returns the square of n.");
		addFunction("Math.sqrt(n)", "Math", "Returns the square root of n.");
		addFunction("Math.tan(angle)", "Math",
			"Returns the tangent of an angle (in radians).");

		// IJ Object Functions
		addFunction("IJ.deleteRows(index1, index2)", "IJ",
			"Deletes rows in the results table.");
		addFunction("IJ.getToolName()", "IJ",
			"Returns the name of the currently selected tool.");
		addFunction("IJ.getFullVersion()", "IJ",
			"Returns the ImageJ version and build number as a string.");
		addFunction("IJ.freeMemory()", "IJ", "Returns the memory status string.");
		addFunction("IJ.checksum(type, arg)", "IJ",
			"Returns the MD5 or SHA-256 checksum.");
		addFunction("IJ.currentMemory()", "IJ",
			"Returns the amount of memory in bytes currently used by ImageJ.");
		addFunction("IJ.log(string)", "IJ", "Displays string in the Log window.");
		addFunction("IJ.maxMemory()", "IJ",
			"Returns the amount of memory in bytes available to ImageJ.");
		addFunction("IJ.pad(n, length)", "IJ", "Pads 'n' with leading zeros.");
		addFunction("IJ.renameResults(name)", "IJ",
			"Changes the title of the Results table.");
		addFunction("IJ.renameResults(oldName, newName)", "IJ",
			"Changes the title of a results table from oldName to newName.");
		addFunction("IJ.redirectErrorMessages()", "IJ",
			"Causes next image opening error to be redirected to the Log window.");

		// Constants
		addFunction("PI", "Constants",
			"Returns π (3.14159265), the ratio of the circumference to the diameter of a circle.");
		addFunction("NaN", "Constants", "Represents 'Not-a-Number'.");
	}

	private static void addFunction(String name, String category,
		String description)
	{
		FUNCTIONS.add(new MacroFunction(name, category, description));
		CATEGORIES.add(category);
	}

	/**
	 * Search for macro functions by query string. The search is case-insensitive
	 * and matches against function name and description.
	 *
	 * @param query The search query
	 * @return A list of matching MacroFunction objects
	 */
	public static List<MacroFunction> search(String query) {
		if (query == null || query.trim().isEmpty()) {
			return FUNCTIONS;
		}

		String lowerQuery = query.toLowerCase();

		return FUNCTIONS.stream().filter(f -> f.getName().toLowerCase().contains(
			lowerQuery) || f.getDescription().toLowerCase().contains(lowerQuery) || f
				.getCategory().toLowerCase().contains(lowerQuery)).collect(Collectors
					.toList());
	}

	/**
	 * Get all functions in a specific category.
	 *
	 * @param category The category name
	 * @return A list of MacroFunction objects in the specified category
	 */
	public static List<MacroFunction> getByCategory(String category) {
		return FUNCTIONS.stream().filter(f -> f.getCategory().equalsIgnoreCase(
			category)).collect(Collectors.toList());
	}

	/**
	 * Get a specific function by exact name match.
	 *
	 * @param name The function name
	 * @return The MacroFunction if found, or null
	 */
	public static MacroFunction getByName(String name) {
		return FUNCTIONS.stream().filter(f -> f.getName().equalsIgnoreCase(name))
			.findFirst().orElse(null);
	}

	/**
	 * Get all available categories.
	 *
	 * @return A list of category names, sorted alphabetically
	 */
	public static List<String> getCategories() {
		return CATEGORIES.stream().sorted().collect(Collectors.toList());
	}

	/**
	 * Get all functions.
	 *
	 * @return A list of all MacroFunction objects
	 */
	public static List<MacroFunction> getAllFunctions() {
		return new ArrayList<>(FUNCTIONS);
	}

	/**
	 * Represents a single ImageJ macro built-in function.
	 */
	public static class MacroFunction {

		private final String name;
		private final String category;
		private final String description;

		public MacroFunction(String name, String category, String description) {
			this.name = name;
			this.category = category;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getCategory() {
			return category;
		}

		public String getDescription() {
			return description;
		}

		/**
		 * @return As {@link #toString()} but without categories
		 */
		public String simpleString() {
			return String.format("%s: %s", name, description);
		}

		@Override
		public String toString() {
			return String.format("%s (%s): %s", name, category, description);
		}
	}
}
